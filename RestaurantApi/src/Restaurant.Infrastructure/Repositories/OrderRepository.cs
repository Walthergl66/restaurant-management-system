using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Orders;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class OrderRepository(ApplicationDbContext dbContext) : IOrderRepository
{
    public async Task<Order?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Order>().FindAsync([id], cancellationToken);
    }

    public async Task<Order?> GetWithItemsAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Order>()
            .Include(o => o.Items)
                .ThenInclude(i => i.Extras)
            .Include(o => o.Items)
                .ThenInclude(i => i.RemovedIngredients)
            .FirstOrDefaultAsync(o => o.Id == id, cancellationToken);
    }

    public async Task<IReadOnlyCollection<Order>> GetOpenAsync(CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Order>()
            .AsNoTracking()
            .Include(o => o.Items)
            .Where(o => o.Status == OrderStatus.DRAFT
                || o.Status == OrderStatus.CONFIRMED
                || o.Status == OrderStatus.IN_PREPARATION
                || o.Status == OrderStatus.READY)
            .OrderByDescending(o => o.CreatedAtUtc)
            .ToListAsync(cancellationToken);
    }

    public async Task AddAsync(Order order, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<Order>().AddAsync(order, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}