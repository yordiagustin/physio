namespace Physio.Core;

public class PhaseAssigned
{
    public Guid Id { get; set; }
    public required Plan Plan { get; set; }
    public required Phase Phase { get; set; }
    public int OrderNumber { get; set; }
    public PhaseAssignedStatus Status { get; set; }
    public bool IsActive { get; set; }
}

public enum PhaseAssignedStatus
{
    NotStarted = 0,
    InProgress = 1,
    Completed = 2
}