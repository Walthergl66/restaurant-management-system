using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Additions;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class AdditionRepository(ApplicationDbContext dbContext) : IAdditionRepository
{
    public async Task<Addition?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Addition>().FindAsync([id], cancellationToken);
    }

    public async Task<Addition?> GetWithItemsAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Addition>()
            .Include(a => a.Items)
                .ThenInclude(i => i.Extras)
            .Include(a => a.Items)
                .ThenInclude(i => i.RemovedIngredients)
            .FirstOrDefaultAsync(a => a.Id == id, cancellationToken);
    }

    public async Task AddAsync(Addition addition, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<Addition>().AddAsync(addition, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}