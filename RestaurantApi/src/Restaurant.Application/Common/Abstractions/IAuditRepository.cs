using Restaurant.Application.Audit;
using Restaurant.Domain.Audit;

namespace Restaurant.Application.Common.Abstractions;

public interface IAuditRepository
{
    Task AddAsync(AuditLog auditLog, CancellationToken cancellationToken = default);

    Task<IReadOnlyCollection<AuditLog>> SearchAsync(AuditQuery query, CancellationToken cancellationToken = default);

    Task<int> SaveChangesAsync(CancellationToken cancellationToken = default);
}
