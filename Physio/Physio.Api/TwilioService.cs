using Twilio;
using Twilio.Rest.Verify.V2.Service;

namespace Physio.Api;

public class TwilioService
{
    private readonly string _verifyServiceSid;

    public TwilioService(IConfiguration configuration)
    {
        var accountSid = configuration["Twilio:AccountSid"];
        var authToken = configuration["Twilio:AuthToken"];
        _verifyServiceSid = configuration["Twilio:VerifyServiceSid"]
                            ?? throw new ArgumentNullException(nameof(_verifyServiceSid));

        TwilioClient.Init(accountSid, authToken);
    }

    public async Task<bool> SendVerificationCodeAsync(string phoneNumber)
    {
        try
        {
            await VerificationResource.CreateAsync(
                to: phoneNumber,
                channel: "sms",
                pathServiceSid: _verifyServiceSid);

            return true;
        }
        catch (Exception)
        {
            return false;
        }
    }

    public async Task<bool> VerifyCodeAsync(string phoneNumber, string code)
    {
        try
        {
            var verificationCheck = await VerificationCheckResource.CreateAsync(
                to: phoneNumber,
                code: code,
                pathServiceSid: _verifyServiceSid);

            return verificationCheck.Status == "approved";
        }
        catch (Exception)
        {
            return false;
        }
    }
}