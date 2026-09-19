using Restaurant.Application.Common;

namespace Restaurant.Application.Audit;

public sealed record AuditEntry(
    Guid? UserId,
    string Action,
    string EntityType,
    Guid? EntityId = null,
    string? OldValues = null,
    string? NewValues = null,
    string? IpAddress = null);

public sealed record AuditLogResponse(
    Guid Id,
    Guid? UserId,
    string Action,
    string EntityType,
    Guid? EntityId,
    DateTime TimestampUtc,
    string? OldValues,
    string? NewValues,
    string? IpAddress);

public sealed record AuditQuery(string? Action = null, string? EntityType = null, Guid? UserId = null,
    DateTime? From = null, DateTime? To = null, int Page = 1, int PageSize = 50);

public interface IAuditService
{
    Task RecordAsync(AuditEntry entry, CancellationToken cancellationToken = default);

    Task<Result<IReadOnlyCollection<AuditLogResponse>>> SearchAsync(AuditQuery query, CancellationToken cancellationToken);
}