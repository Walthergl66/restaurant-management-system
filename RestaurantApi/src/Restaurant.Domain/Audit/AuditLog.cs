using Restaurant.Domain.Common;

namespace Restaurant.Domain.Audit;

public sealed class AuditLog : BaseEntity
{
    public Guid? UserId { get; private set; }

    public string Action { get; private set; } = string.Empty;

    public string EntityType { get; private set; } = string.Empty;

    public Guid? EntityId { get; private set; }

    public DateTime TimestampUtc { get; private set; } = DateTime.UtcNow;

    public string? OldValues { get; private set; }

    public string? NewValues { get; private set; }

    public string? IpAddress { get; private set; }

    public static AuditLog Create(
        Guid? userId,
        string action,
        string entityType,
        Guid? entityId = null,
        string? oldValues = null,
        string? newValues = null,
        string? ipAddress = null)
    {
        if (string.IsNullOrWhiteSpace(action))
        {
            throw new DomainException("La acción del log es obligatoria.");
        }

        if (string.IsNullOrWhiteSpace(entityType))
        {
            throw new DomainException("El tipo de entidad del log es obligatorio.");
        }

        return new AuditLog
        {
            UserId = userId,
            Action = action.Trim(),
            EntityType = entityType.Trim(),
            EntityId = entityId,
            OldValues = oldValues,
            NewValues = newValues,
            IpAddress = ipAddress,
        };
    }
}