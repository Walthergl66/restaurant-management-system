using Restaurant.Domain.Catalog;

namespace Restaurant.Application.Common.Abstractions;

public interface IProductRepository
{
    Task<IReadOnlyCollection<Product>> GetAllAsync(bool onlyAvailable, CancellationToken cancellationToken = default);

    Task<Product?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task<Product?> GetWithDetailsAsync(Guid id, CancellationToken cancellationToken = default);

    Task AddAsync(Product product, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}