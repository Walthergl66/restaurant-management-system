using Restaurant.Domain.Catalog;

namespace Restaurant.Application.Common.Abstractions;

public interface ICategoryRepository
{
    Task<IReadOnlyCollection<Category>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken = default);

    Task<Category?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task AddAsync(Category category, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}