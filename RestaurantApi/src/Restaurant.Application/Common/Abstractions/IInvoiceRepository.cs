using Restaurant.Domain.Billing;

namespace Restaurant.Application.Common.Abstractions;

public interface IInvoiceRepository
{
    Task<Invoice?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task<Invoice?> GetWithItemsAsync(Guid id, CancellationToken cancellationToken = default);

    Task<Invoice?> GetBySaleAsync(Guid saleId, CancellationToken cancellationToken = default);

    Task AddAsync(Invoice invoice, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}