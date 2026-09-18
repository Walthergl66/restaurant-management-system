using Restaurant.Application.Common;
using Restaurant.Domain.Orders;
using Restaurant.Domain.Preparation;

namespace Restaurant.Application.Preparation;

public sealed record PreparationOrderItemResponse(
    Guid Id,
    Guid SourceOrderItemId,
    Guid ProductId,
    string ProductName,
    int Quantity,
    string? Notes);

public sealed record PreparationOrderResponse(
    Guid Id,
    Guid SourceOrderId,
    Guid PreparationAreaId,
    string PreparationAreaName,
    string Status,
    DateTime? PrintedAtUtc,
    DateTime? StartedAtUtc,
    DateTime? ReadyAtUtc,
    IReadOnlyCollection<PreparationOrderItemResponse> Items);

public interface IPreparationOrderService
{
    Task<Result<IReadOnlyCollection<PreparationOrderResponse>>> GetByStatusAsync(int status, CancellationToken cancellationToken);

    Task<Result<PreparationOrderResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<IReadOnlyCollection<PreparationOrderResponse>>> GenerateAsync(Order order, CancellationToken cancellationToken);

    Task<Result> StartAsync(Guid id, CancellationToken cancellationToken);

    Task<Result> MarkReadyAsync(Guid id, CancellationToken cancellationToken);

    Task<Result> CancelAsync(Guid id, CancellationToken cancellationToken);
}