namespace Physio.Core;

public class Exercise
{
    public Guid Id { get; set; }
    public required string Name { get; set; }
    public string? Description { get; set; }
    public int EstimatedDuration { get; set; }
    public int Repetitions { get; set; }
    public DifficultyLevel Difficulty { get; set; }
    public string? Instructions { get; set; }
}

public enum DifficultyLevel
{
    Easy = 0,
    Medium = 1,
    Hard = 2
}