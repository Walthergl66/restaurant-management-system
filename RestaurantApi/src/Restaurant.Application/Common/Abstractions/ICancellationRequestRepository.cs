using Restaurant.Domain.Cancellations;

namespace Restaurant.Application.Common.Abstractions;

public interface ICancellationRequestRepository
{
    Task<CancellationRequest?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task<IReadOnlyCollection<CancellationRequest>> GetPendingAsync(CancellationToken cancellationToken = default);

    Task AddAsync(CancellationRequest request, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}