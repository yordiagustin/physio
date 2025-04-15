using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Mvc;
using Twilio;
using Twilio.Rest.Verify.V2;
using Twilio.Rest.Verify.V2.Service;

namespace Physio.Api.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController(DatabaseService databaseService, TwilioService twilioService) : ControllerBase
{
    [HttpPost("send-code")]
    public async Task<IActionResult> AuthenticateUser([FromBody] SendCodeRequest request)
    {
        if (!ModelState.IsValid)
            return BadRequest(new { Message = "Invalid phone number format." });

        var exists = await databaseService.PatientPhoneNumberExistsAsync(request.PhoneNumber);
        if (!exists)
            return BadRequest(new { Message = "Phone number is not registered." });

        try
        {
            await twilioService.SendVerificationCodeAsync(request.PhoneNumber);
            return Ok(new { Message = $"Verification code sent to {request.PhoneNumber}" });
        }
        catch (Exception)
        {
            return StatusCode(500, new { Message = "Error occurred while sending the verification code." });
        }
    }

    [HttpPost("verify-code")]
    public async Task<IActionResult> VerifyAuthCode([FromBody] VerificationCodeRequest request)
    {
        if (!ModelState.IsValid)
            return BadRequest(new { Message = "Invalid request data." });

        var exists = await databaseService.PatientPhoneNumberExistsAsync(request.PhoneNumber);
        if (!exists)
            return BadRequest(new { Message = "Phone number is not registered." });

        try
        {
            var isValid = await twilioService.VerifyCodeAsync(request.PhoneNumber, request.VerificationCode);
            return isValid
                ? Ok(new { Message = "Verification successful." })
                : BadRequest(new { Message = "Invalid verification code." });
        }
        catch (Exception)
        {
            return StatusCode(500, new { Message = "Error occurred while validating the verification code." });
        }
    }

    public class SendCodeRequest
    {
        [RegularExpression(@"^\+[1-9]\d{1,14}$", ErrorMessage = "Phone Number must be in E.164 Format.")]
        public required string PhoneNumber { get; init; }
    }

    public class VerificationCodeRequest
    {
        [RegularExpression(@"^\+[1-9]\d{1,14}$", ErrorMessage = "Phone Number must be in E.164 Format.")]
        public required string PhoneNumber { get; init; }

        public required string VerificationCode { get; init; }
    }
}