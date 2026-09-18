namespace Restaurant.Domain.Common;

public abstract class AuditableEntity : BaseEntity
{
    public DateTime CreatedAtUtc { get; protected set; } = DateTime.UtcNow;

    public DateTime? UpdatedAtUtc { get; protected set; }

    public Guid? CreatedBy { get; protected set; }

    public Guid? UpdatedBy { get; protected set; }

    public void MarkModified(Guid? updatedBy = null)
    {
        UpdatedAtUtc = DateTime.UtcNow;
        UpdatedBy = updatedBy;
    }
}