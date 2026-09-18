using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Catalog;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class ExtraRepository(ApplicationDbContext dbContext) : IExtraRepository
{
    public async Task<IReadOnlyCollection<Extra>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken = default)
    {
        var query = dbContext.Set<Extra>().AsNoTracking();

        if (onlyActive)
        {
            query = query.Where(e => e.IsActive);
        }

        return await query
            .OrderBy(e => e.Name)
            .ToListAsync(cancellationToken);
    }

    public async Task<Extra?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Extra>().FindAsync([id], cancellationToken);
    }

    public async Task AddAsync(Extra extra, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<Extra>().AddAsync(extra, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}