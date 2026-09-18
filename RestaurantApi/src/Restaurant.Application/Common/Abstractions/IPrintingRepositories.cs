using Restaurant.Domain.Printing;

namespace Restaurant.Application.Common.Abstractions;

public interface IPrinterRepository
{
    Task<Printer?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task<IReadOnlyCollection<Printer>> GetActiveAsync(CancellationToken cancellationToken = default);

    Task<Printer?> GetForAreaAsync(Guid preparationAreaId, CancellationToken cancellationToken = default);

    Task AddAsync(Printer printer, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}

public interface IPrintJobRepository
{
    Task<PrintJob?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task<PrintJob?> GetByIdempotencyKeyAsync(string idempotencyKey, CancellationToken cancellationToken = default);

    Task AddAsync(PrintJob printJob, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}