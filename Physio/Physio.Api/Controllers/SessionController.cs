using Dapper;
using Microsoft.AspNetCore.Mvc;

namespace Physio.Api.Controllers;

[ApiController]
[Route("api/session")]
public class SessionController(IConfiguration configuration) : ControllerBase
{
    private readonly string _connectionString = configuration.GetConnectionString("DefaultConnection") ??
                                                throw new ArgumentNullException($"Connection String is required");

    [HttpPost]
    public async Task<IActionResult> SaveSessionResults([FromBody] SessionResultsRequest request)
    {
        try
        {
            await using var connection = new Npgsql.NpgsqlConnection(_connectionString);
            await connection.OpenAsync();
            await using var transaction = await connection.BeginTransactionAsync();

            try
            {
                const string sessionQuery = """
                    INSERT INTO exercise_sessions 
                    (session_uuid, patient_id, exercise_id, session_start, session_end, 
                     total_reps, successful_reps, total_errors, session_score, notes)
                    VALUES 
                    (@SessionUuid, @PatientId, @ExerciseId, @SessionStart, @SessionEnd, 
                     @TotalReps, @SuccessfulReps, @TotalErrors, @SessionScore, @Notes)
                    RETURNING id;
                    """;

                var sessionId = await connection.QuerySingleAsync<int>(sessionQuery, new
                {
                    SessionUuid = Guid.Parse(request.SessionUuid),
                    PatientId = Guid.Parse(request.PatientId),
                    request.ExerciseId,
                    SessionStart = DateTimeOffset.FromUnixTimeMilliseconds(request.SessionStart),
                    SessionEnd = request.SessionEnd.HasValue 
                        ? DateTimeOffset.FromUnixTimeMilliseconds(request.SessionEnd.Value) 
                        : (DateTimeOffset?)null,
                    request.TotalReps,
                    request.SuccessfulReps,
                    request.TotalErrors,
                    request.SessionScore,
                    request.Notes
                }, transaction);
                
                foreach (var rep in request.Repetitions)
                {
                    const string repQuery = """
                        INSERT INTO exercise_repetitions 
                        (session_id, rep_number, effective_time_ms, range_of_motion_degrees, 
                         error_count, max_angle_reached, min_angle_reached, 
                         rep_start_time, rep_end_time, quality_score)
                        VALUES 
                        (@SessionId, @RepNumber, @EffectiveTimeMs, @RangeOfMotionDegrees, 
                         @ErrorCount, @MaxAngleReached, @MinAngleReached, 
                         @RepStartTime, @RepEndTime, @QualityScore)
                        RETURNING id;
                        """;

                    var repetitionId = await connection.QuerySingleAsync<int>(repQuery, new
                    {
                        SessionId = sessionId,
                        rep.RepNumber,
                        rep.EffectiveTimeMs,
                        rep.RangeOfMotionDegrees,
                        rep.ErrorCount,
                        rep.MaxAngleReached,
                        rep.MinAngleReached,
                        RepStartTime = DateTimeOffset.FromUnixTimeMilliseconds(rep.StartTime),
                        RepEndTime = rep.EndTime.HasValue 
                            ? DateTimeOffset.FromUnixTimeMilliseconds(rep.EndTime.Value) 
                            : (DateTimeOffset?)null,
                        rep.QualityScore
                    }, transaction);
                    
                    foreach (var error in rep.Errors)
                    {
                        const string errorQuery = """
                            INSERT INTO repetition_errors 
                            (repetition_id, error_code, detected_at)
                            VALUES 
                            (@RepetitionId, @ErrorCode, @DetectedAt)
                            ON CONFLICT (repetition_id, error_code) DO NOTHING;
                            """;

                        await connection.ExecuteAsync(errorQuery, new
                        {
                            RepetitionId = repetitionId,
                            error.ErrorCode,
                            DetectedAt = DateTimeOffset.FromUnixTimeMilliseconds(error.DetectedAt)
                        }, transaction);
                    }
                }

                await transaction.CommitAsync();

                return Ok(new { 
                    message = "Session saved successfully",
                    sessionId,
                    sessionUuid = request.SessionUuid
                });
            }
            catch (Exception)
            {
                await transaction.RollbackAsync();
                throw;
            }
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = "Failed to save session", details = ex.Message });
        }
    }
    
    [HttpGet("results")]
    public async Task<IActionResult> GetSessionResults([FromQuery] Guid sessionId)
    {
        try
        {
            await using var connection = new Npgsql.NpgsqlConnection(_connectionString);
            const string query = """
                                 SELECT 
                                     es.session_uuid as SessionUuid,
                                     p.name || ' ' || p.surname as PatientName,
                                     e.name as ExerciseName,
                                     
                                     -- SESSION DATE (readable format)
                                     DATE(es.session_start) as SessionDate,
                                     es.session_start as SessionStart,
                                     es.session_end as SessionEnd,
                                     
                                     -- TOTAL TIME (in minutes, rounded)
                                     ROUND(
                                         EXTRACT(EPOCH FROM (COALESCE(es.session_end, CURRENT_TIMESTAMP) - es.session_start)) / 60.0, 
                                         1
                                     ) as TotalDurationMinutes,
                                     
                                     -- COMPLETED REPETITIONS
                                     es.total_reps as RepetitionsCompleted,
                                     
                                     -- TOTAL ERRORS
                                     es.total_errors as TotalErrors,
                                     
                                     -- AVERAGE RANGE OF MOTION (only repetitions with range <= 90°)
                                     ROUND(
                                         COALESCE(
                                             AVG(er.range_of_motion_degrees) FILTER (WHERE er.range_of_motion_degrees <= 90),
                                             0
                                         ), 1
                                     ) as AvgRangeOfMotion,
                                     
                                     -- SESSION SCORE (convert to percentage if it's between 0-1)
                                     CASE 
                                         WHEN es.session_score <= 1.0 THEN ROUND(es.session_score * 100, 1)
                                         ELSE ROUND(es.session_score, 1)
                                     END as SessionScore

                                 FROM exercise_sessions es
                                 JOIN exercises e ON es.exercise_id = e.id
                                 JOIN patient p ON es.patient_id = p.id
                                 LEFT JOIN exercise_repetitions er ON es.id = er.session_id
                                 WHERE es.session_uuid = @SessionId
                                 GROUP BY es.id, p.name, p.surname, e.name, es.session_start, es.session_end;
                                 """;

            var result = await connection.QuerySingleOrDefaultAsync<SessionResults>(query, new { SessionId = sessionId });
            
            if (result == null)
            {
                return NotFound(new { message = "Session not found", sessionId });
            }
            
            return Ok(result);
        }
        catch (Exception ex)
        {
            // Log the exception here if you have a logger
            return StatusCode(500, new { message = "Database operation failed.", error = ex.Message });
        }
    }
}

public record SessionResultsRequest(
    string SessionUuid,
    string PatientId,
    int ExerciseId,
    long SessionStart,
    long? SessionEnd,
    int TotalReps,
    int SuccessfulReps,
    int TotalErrors,
    decimal SessionScore,
    string? Notes,
    List<RepetitionRequest> Repetitions
);

// ReSharper disable once ClassNeverInstantiated.Global
public record RepetitionRequest(
    int RepNumber,
    long StartTime,
    long? EndTime,
    int EffectiveTimeMs,
    decimal RangeOfMotionDegrees,
    int ErrorCount,
    decimal MaxAngleReached,
    decimal MinAngleReached,
    decimal QualityScore,
    List<RepetitionErrorRequest> Errors
);

// ReSharper disable once ClassNeverInstantiated.Global
public record RepetitionErrorRequest(
    string ErrorCode,
    long DetectedAt
);

public record SessionResults(
    Guid SessionUuid,
    string PatientName,
    string ExerciseName,
    DateTime SessionDate,
    DateTime SessionStart,
    DateTime? SessionEnd,
    decimal TotalDurationMinutes,
    int RepetitionsCompleted,
    int TotalErrors,
    decimal AvgRangeOfMotion,
    decimal SessionScore
);