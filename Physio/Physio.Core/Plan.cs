namespace Physio.Core;

public class Plan
{
    public Guid Id { get; set; }
    public required string Diagnosis { get; set; }
    public required Patient Patient { get; set; }
}