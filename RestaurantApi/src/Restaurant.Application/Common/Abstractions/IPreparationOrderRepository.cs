using Restaurant.Domain.Preparation;

namespace Restaurant.Application.Common.Abstractions;

public interface IPreparationOrderRepository
{
    Task<PreparationOrder?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task<PreparationOrder?> GetWithItemsAsync(Guid id, CancellationToken cancellationToken = default);

    Task<IReadOnlyCollection<PreparationOrder>> GetByStatusAsync(PreparationOrderStatus status, CancellationToken cancellationToken = default);

    Task<IReadOnlyCollection<PreparationOrder>> GetBySourceOrderAsync(Guid sourceOrderId, CancellationToken cancellationToken = default);

    Task AddRangeAsync(IEnumerable<PreparationOrder> orders, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}