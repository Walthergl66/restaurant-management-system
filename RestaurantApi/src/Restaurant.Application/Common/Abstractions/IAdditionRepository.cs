using Restaurant.Domain.Additions;

namespace Restaurant.Application.Common.Abstractions;

public interface IAdditionRepository
{
    Task<Addition?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task<Addition?> GetWithItemsAsync(Guid id, CancellationToken cancellationToken = default);

    Task AddAsync(Addition addition, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}