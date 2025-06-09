using Dapper;
using Microsoft.AspNetCore.Mvc;

namespace Physio.Api.Controllers;

[ApiController]
[Route("api/exercises")]
public class ExercisesController(IConfiguration configuration) : ControllerBase
{
    private readonly string _connectionString = configuration.GetConnectionString("DefaultConnection") ??
                                                throw new ArgumentNullException($"Connection String is required");

    [HttpGet]
    public async Task<IActionResult> GetAssignedByPatient([FromQuery] Guid patientId)
    {
        try
        {
            await using var connection = new Npgsql.NpgsqlConnection(_connectionString);
            const string query = """
                                 SELECT
                                     e.id as ExerciseId,
                                     e.name as ExerciseName,
                                     e.description as Description,
                                     e.estimated_duration_minutes as EstimatedDurationMinutes,
                                     e.difficulty_level as DifficultyLevel,
                                     e.instructions as Instructions,
                                     pea.target_reps_per_session as Repetitions
                                 FROM patient_exercise_assignments pea
                                 JOIN exercises e ON pea.exercise_id = e.id
                                 WHERE pea.patient_id = @PatientId
                                   AND pea.is_active = true
                                   AND e.is_active = true
                                 ORDER BY pea.assigned_date DESC;
                                 """;

            var result = 
                await connection.QueryAsync<ExercisesAssignedByPatient>(query, new { PatientId = patientId });
            
            return Ok(result);
        }
        catch (Exception ex)
        {
            throw new Exception("Database operation failed.", ex);
        }
    }
    
    public record ExercisesAssignedByPatient(
        int ExerciseId,
        string ExerciseName,
        string Description,
        int EstimatedDurationMinutes,
        int DifficultyLevel,
        string Instructions,
        int Repetitions);
}