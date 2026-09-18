using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Catalog;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class ProductRepository(ApplicationDbContext dbContext) : IProductRepository
{
    public async Task<IReadOnlyCollection<Product>> GetAllAsync(bool onlyAvailable, CancellationToken cancellationToken = default)
    {
        IQueryable<Product> query = dbContext.Set<Product>()
            .Include(p => p.Category)
            .Include(p => p.PreparationArea)
            .Include(p => p.Prices);

        if (onlyAvailable)
        {
            query = query.Where(p => p.IsAvailable);
        }

        return await query
            .OrderBy(p => p.Name)
            .ToListAsync(cancellationToken);
    }

    public async Task<Product?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Product>().FindAsync([id], cancellationToken);
    }

    public async Task<Product?> GetWithDetailsAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Product>()
            .Include(p => p.Category)
            .Include(p => p.PreparationArea)
            .Include(p => p.Prices)
            .Include(p => p.AvailableExtras)
                .ThenInclude(e => e.Extra)
            .Include(p => p.Ingredients)
            .FirstOrDefaultAsync(p => p.Id == id, cancellationToken);
    }

    public async Task AddAsync(Product product, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<Product>().AddAsync(product, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}