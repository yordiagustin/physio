using Microsoft.AspNetCore.Mvc;

namespace Physio.Api.Controllers;

[ApiController]
[Route("api/exercises")]
public class ExercisesController(DatabaseService databaseService) : ControllerBase
{
    [HttpGet("byDate")]
    public async Task<IActionResult> GetRoutineExercisesByDate([FromQuery] DateTime date, Guid phaseId)
    {
        var result = await databaseService.GetRoutineExercisesByDateAsync(date, phaseId);
        if (result is null)
            return NotFound(new { Message = "Exercises not found." });
        
        return Ok(result);
    }
    
    [HttpGet]
    public async Task<IActionResult> GetExerciseById([FromQuery] Guid id)
    {   
        var result = await databaseService.GetExerciseById(id);
        if (result is null)
            return NotFound(new { Message = "Exercise not found." });
        
        return Ok(result);
    }
}