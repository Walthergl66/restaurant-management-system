using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Catalog;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class PreparationAreaRepository(ApplicationDbContext dbContext) : IPreparationAreaRepository
{
    public async Task<IReadOnlyCollection<PreparationArea>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken = default)
    {
        var query = dbContext.Set<PreparationArea>().AsNoTracking();

        if (onlyActive)
        {
            query = query.Where(a => a.IsActive);
        }

        return await query
            .OrderBy(a => a.Name)
            .ToListAsync(cancellationToken);
    }

    public async Task<PreparationArea?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<PreparationArea>().FindAsync([id], cancellationToken);
    }

    public async Task AddAsync(PreparationArea preparationArea, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<PreparationArea>().AddAsync(preparationArea, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}