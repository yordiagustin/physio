namespace Physio.Core;

public class Routine
{
    public Guid Id { get; set; }
    public required Phase Phase { get; set; }
    public required Exercise Exercise { get; set; }
    public DateOnly ExecutionDate { get; set; }
    public int OrderNumber { get; set; }
}