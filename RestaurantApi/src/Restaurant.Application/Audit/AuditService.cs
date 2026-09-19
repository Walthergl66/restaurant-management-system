using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Audit;

namespace Restaurant.Application.Audit;

public sealed class AuditService(IAuditRepository auditRepository) : IAuditService
{
    public async Task RecordAsync(AuditEntry entry, CancellationToken cancellationToken)
    {
        var log = AuditLog.Create(
            entry.UserId,
            entry.Action,
            entry.EntityType,
            entry.EntityId,
            entry.OldValues,
            entry.NewValues,
            entry.IpAddress);

        await auditRepository.AddAsync(log, cancellationToken);
        await auditRepository.SaveChangesAsync(cancellationToken);
    }

    public async Task<Result<IReadOnlyCollection<AuditLogResponse>>> SearchAsync(AuditQuery query, CancellationToken cancellationToken)
    {
        var logs = await auditRepository.SearchAsync(query, cancellationToken);

        return logs
            .Select(l => new AuditLogResponse(
                l.Id,
                l.UserId,
                l.Action,
                l.EntityType,
                l.EntityId,
                l.TimestampUtc,
                l.OldValues,
                l.NewValues,
                l.IpAddress))
            .ToList();
    }
}
