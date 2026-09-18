using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Cancellations;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class CancellationRequestRepository(ApplicationDbContext dbContext) : ICancellationRequestRepository
{
    public async Task<CancellationRequest?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<CancellationRequest>().FindAsync([id], cancellationToken);
    }

    public async Task<IReadOnlyCollection<CancellationRequest>> GetPendingAsync(CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<CancellationRequest>()
            .AsNoTracking()
            .Where(r => r.Status == CancellationRequestStatus.PENDING)
            .OrderByDescending(r => r.CreatedAtUtc)
            .ToListAsync(cancellationToken);
    }

    public async Task AddAsync(CancellationRequest request, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<CancellationRequest>().AddAsync(request, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}