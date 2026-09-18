using Restaurant.Domain.Catalog;

namespace Restaurant.Application.Common.Abstractions;

public interface IExtraRepository
{
    Task<IReadOnlyCollection<Extra>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken = default);

    Task<Extra?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task AddAsync(Extra extra, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}