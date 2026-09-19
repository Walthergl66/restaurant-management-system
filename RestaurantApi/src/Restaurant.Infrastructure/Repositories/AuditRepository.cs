using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Audit;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Audit;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class AuditRepository(ApplicationDbContext dbContext) : IAuditRepository
{
    public async Task AddAsync(AuditLog auditLog, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<AuditLog>().AddAsync(auditLog, cancellationToken);
    }

    public async Task<IReadOnlyCollection<AuditLog>> SearchAsync(AuditQuery query, CancellationToken cancellationToken = default)
    {
        var filtered = dbContext.Set<AuditLog>()
            .AsNoTracking()
            .Where(l => query.Action is null || l.Action == query.Action)
            .Where(l => query.EntityType is null || l.EntityType == query.EntityType)
            .Where(l => query.UserId is null || l.UserId == query.UserId)
            .Where(l => query.From is null || l.TimestampUtc >= query.From.Value)
            .Where(l => query.To is null || l.TimestampUtc <= query.To.Value)
            .OrderByDescending(l => l.TimestampUtc);

        return await filtered
            .Skip((Math.Max(query.Page, 1) - 1) * Math.Clamp(query.PageSize, 1, 200))
            .Take(Math.Clamp(query.PageSize, 1, 200))
            .ToListAsync(cancellationToken);
    }

    public Task<int> SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}