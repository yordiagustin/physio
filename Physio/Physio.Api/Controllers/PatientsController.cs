using Microsoft.AspNetCore.Mvc;

namespace Physio.Api.Controllers;

[ApiController]
[Route("api/patients")]
public class PatientsController(DatabaseService databaseService) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> GetPatientByPhoneNumber([FromQuery] string phoneNumber)
    {
        var result = await databaseService.GetPatientByPhoneNumberAsync(phoneNumber);
        if (result is null)
            return NotFound(new { Message = "Exercises not found." });
        
        return Ok(result);
    }
}