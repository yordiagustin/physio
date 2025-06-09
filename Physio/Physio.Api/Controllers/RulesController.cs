using System.Text.Json;
using Dapper;
using Microsoft.AspNetCore.Mvc;

namespace Physio.Api.Controllers;

[ApiController]
[Route("api/exercises/rules")]
public class RulesController(IConfiguration configuration) : ControllerBase
{
    private readonly string _connectionString = configuration.GetConnectionString("DefaultConnection") ??
                                                throw new ArgumentNullException($"Connection String is required");

    [HttpGet]
    public async Task<IActionResult> GetByExercise([FromQuery] int exerciseId)
    {
        try
        {
            await using var connection = new Npgsql.NpgsqlConnection(_connectionString);
            const string query = "SELECT get_exercise_rules(@ExerciseId);";

            var jsonResult = await connection.QuerySingleOrDefaultAsync<string>(query, new { ExerciseId = exerciseId });

            if (jsonResult is null) return NotFound($"Exercise with ID {exerciseId} not found or inactive");

            var exerciseRules = JsonSerializer.Deserialize<object>(jsonResult);

            return Ok(exerciseRules);
        }
        catch (JsonException ex)
        {
            return StatusCode(500, new { error = "Invalid JSON format", details = ex.Message });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = "Database operation failed", details = ex.Message });
        }
    }
}