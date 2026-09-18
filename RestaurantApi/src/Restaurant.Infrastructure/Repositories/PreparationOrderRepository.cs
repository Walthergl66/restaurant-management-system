using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Preparation;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class PreparationOrderRepository(ApplicationDbContext dbContext) : IPreparationOrderRepository
{
    public async Task<PreparationOrder?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<PreparationOrder>().FindAsync([id], cancellationToken);
    }

    public async Task<PreparationOrder?> GetWithItemsAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<PreparationOrder>()
            .Include(o => o.Items)
            .FirstOrDefaultAsync(o => o.Id == id, cancellationToken);
    }

    public async Task<IReadOnlyCollection<PreparationOrder>> GetByStatusAsync(PreparationOrderStatus status, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<PreparationOrder>()
            .AsNoTracking()
            .Include(o => o.Items)
            .Where(o => o.Status == status)
            .OrderBy(o => o.CreatedAtUtc)
            .ToListAsync(cancellationToken);
    }

    public async Task<IReadOnlyCollection<PreparationOrder>> GetBySourceOrderAsync(Guid sourceOrderId, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<PreparationOrder>()
            .AsNoTracking()
            .Include(o => o.Items)
            .Where(o => o.SourceOrderId == sourceOrderId)
            .OrderBy(o => o.CreatedAtUtc)
            .ToListAsync(cancellationToken);
    }

    public async Task AddRangeAsync(IEnumerable<PreparationOrder> orders, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<PreparationOrder>().AddRangeAsync(orders, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}