namespace Physio.Core;

public class Session
{
    public Guid Id { get; set; }
    public DateTime StartTime { get; set; }
    public DateTime EndTime { get; set; }
    public TimeSpan TotalTime { get; set; }
    public int CorrectRepetitions { get; set; }
    public int ErrorCount { get; set; }
    public float AverageAccuracy { get; set; }
}