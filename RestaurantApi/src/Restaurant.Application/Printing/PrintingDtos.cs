using Restaurant.Application.Common;

namespace Restaurant.Application.Printing;

public sealed record PrinterResponse(Guid Id, string Name, string? Description, string? IpAddress, string? Port, bool IsActive);

public sealed record CreatePrinterRequest(string Name, string? Description, string? IpAddress, string? Port);

public sealed record UpdatePrinterRequest(string Name, string? Description, string? IpAddress, string? Port, bool IsActive);

public sealed record AssignPrinterAreaRequest(Guid PreparationAreaId);

public sealed record PrintJobResponse(
    Guid Id,
    Guid? SourceOrderId,
    Guid? PreparationOrderId,
    Guid? PrinterId,
    string Status,
    int Attempts,
    string IdempotencyKey,
    string? Error,
    DateTime? CompletedAtUtc);

public interface IPrinterService
{
    Task<Result<IReadOnlyCollection<PrinterResponse>>> GetActiveAsync(CancellationToken cancellationToken);

    Task<Result<PrinterResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<PrinterResponse>> CreateAsync(CreatePrinterRequest request, CancellationToken cancellationToken);

    Task<Result<PrinterResponse>> UpdateAsync(Guid id, UpdatePrinterRequest request, CancellationToken cancellationToken);

    Task<Result> AssignAreaAsync(Guid id, Guid preparationAreaId, CancellationToken cancellationToken);

    Task<Result> UnassignAreaAsync(Guid id, Guid preparationAreaId, CancellationToken cancellationToken);
}

public interface IPrintJobService
{
    Task<Result<PrintJobResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<PrintJobResponse>> CreateAsync(
        Guid? sourceOrderId,
        Guid? preparationOrderId,
        Guid printerId,
        string idempotencyKey,
        CancellationToken cancellationToken);

    Task<Result> MarkSuccessAsync(Guid id, CancellationToken cancellationToken);

    Task<Result> MarkFailedAsync(Guid id, string? error, CancellationToken cancellationToken);
}