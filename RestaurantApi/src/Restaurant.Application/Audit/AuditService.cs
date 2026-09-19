using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Audit;

namespace Restaurant.Application.Audit;

public sealed record AuditQuery(
    string? Action = null,
    string? EntityType = null,
    Guid? UserId = null,
    DateTime? FromUtc = null,
    DateTime? ToUtc = null,
    int Page = 1,
    int PageSize = 50);

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

public interface IAuditService
{
    Task<Result> RecordAsync(AuditEntry entry, CancellationToken cancellationToken = default);

    Task<Result<IReadOnlyCollection<AuditLogResponse>>> SearchAsync(AuditQuery query, CancellationToken cancellationToken = default);
}

public sealed class AuditService(IAuditRepository auditRepository) : IAuditService
{
    public Task<Result> RecordAsync(AuditEntry entry, CancellationToken cancellationToken)
    {
        try
        {
            var log = AuditLog.Create(
                entry.UserId,
                entry.Action,
                entry.EntityType,
                entry.EntityId,
                entry.OldValues,
                entry.NewValues,
                entry.IpAddress);

            return auditRepository
                .AddAsync(log, cancellationToken)
                .ContinueWith(_ => auditRepository.SaveChangesAsync(cancellationToken))
                .Unwrap()
                .ContinueWith(save => save.Result > 0
                    ? Result.Success()
                    : Result.Failure("audit.failed", "No se pudo persistir el registro de auditoría."), CancellationToken.None);
        }
        catch (DomainException exception)
        {
            return Task.FromResult(Result.Failure("audit.invalid", exception.Message));
        }
    }

    public async Task<Result<IReadOnlyCollection<AuditLogResponse>>> SearchAsync(AuditQuery query, CancellationToken cancellationToken)
    {
        var logs = await auditRepository.SearchAsync(query, cancellationToken);

        return logs
            .Select(l => new AuditLogResponse(l.Id, l.UserId, l.Action, l.EntityType, l.EntityId, l.TimestampUtc, l.OldValues, l.NewValues, l.IpAddress))
            .ToList();
    }
}