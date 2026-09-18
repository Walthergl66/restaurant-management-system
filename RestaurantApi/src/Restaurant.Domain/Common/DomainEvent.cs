namespace Restaurant.Domain.Common;

public abstract record DomainEvent
{
    public DateTime OccurredAtUtc { get; } = DateTime.UtcNow;
}