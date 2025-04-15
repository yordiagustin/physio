namespace Physio.Core;

public class Phase
{
    public Guid Id { get; set; }
    public int Number { get; set; }
    public string? Description { get; set; }
    public int EstimatedDuration { get; set; }
    public string? FrequencyDescription { get; set; }
}