using Microsoft.AspNetCore.Mvc;

namespace Physio.Api.Controllers;

[ApiController]
[Route("api/routines")]
public class RoutinesController(DatabaseService databaseService) : ControllerBase
{
    
}