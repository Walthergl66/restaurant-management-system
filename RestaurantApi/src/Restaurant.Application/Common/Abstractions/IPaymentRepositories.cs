using Restaurant.Domain.Payments;
using Restaurant.Domain.Sales;

namespace Restaurant.Application.Common.Abstractions;

public interface IPaymentRepository
{
    Task<Payment?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task AddAsync(Payment payment, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}

public interface ISaleRepository
{
    Task<Sale?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task<Sale?> GetWithItemsAsync(Guid id, CancellationToken cancellationToken = default);

    Task<Sale?> GetByAccountAsync(Guid accountId, CancellationToken cancellationToken = default);

    Task AddAsync(Sale sale, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}