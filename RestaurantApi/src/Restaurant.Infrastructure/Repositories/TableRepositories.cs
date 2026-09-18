using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Tables;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class RestaurantTableRepository(ApplicationDbContext dbContext) : IRestaurantTableRepository
{
    public async Task<IReadOnlyCollection<RestaurantTable>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken = default)
    {
        var query = dbContext.Set<RestaurantTable>().AsNoTracking();

        if (onlyActive)
        {
            query = query.Where(t => t.Status != TableStatus.MAINTENANCE);
        }

        return await query
            .OrderBy(t => t.Name)
            .ToListAsync(cancellationToken);
    }

    public async Task<RestaurantTable?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<RestaurantTable>().FindAsync([id], cancellationToken);
    }

    public async Task AddAsync(RestaurantTable table, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<RestaurantTable>().AddAsync(table, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}

public sealed class TableAccountRepository(ApplicationDbContext dbContext) : ITableAccountRepository
{
    public async Task<TableAccount?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<TableAccount>().FindAsync([id], cancellationToken);
    }

    public async Task<TableAccount?> GetByNumberAsync(string accountNumber, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<TableAccount>()
            .FirstOrDefaultAsync(a => a.AccountNumber == accountNumber, cancellationToken);
    }

    public async Task<IReadOnlyCollection<TableAccount>> GetOpenAccountsAsync(CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<TableAccount>()
            .AsNoTracking()
            .Where(a => a.Status == AccountStatus.OPEN || a.Status == AccountStatus.PAYMENT_PENDING)
            .OrderBy(a => a.OpenedAt)
            .ToListAsync(cancellationToken);
    }

    public async Task AddAsync(TableAccount account, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<TableAccount>().AddAsync(account, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}