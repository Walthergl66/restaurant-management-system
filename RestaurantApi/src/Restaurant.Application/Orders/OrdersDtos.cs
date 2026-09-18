using Restaurant.Application.Common;
using Restaurant.Domain.Catalog;
using Restaurant.Domain.Orders;

namespace Restaurant.Application.Orders;

public sealed record OrderItemExtraResponse(Guid ExtraId, string Name, decimal Price, int Quantity);

public sealed record OrderItemRemovedIngredientResponse(Guid Id, string IngredientName);

public sealed record OrderItemResponse(
    Guid Id,
    Guid ProductId,
    string ProductName,
    Guid PreparationAreaId,
    decimal UnitPrice,
    int Quantity,
    decimal ExtrasTotal,
    decimal LineTotal,
    IReadOnlyCollection<OrderItemExtraResponse> Extras,
    IReadOnlyCollection<OrderItemRemovedIngredientResponse> RemovedIngredients);

public sealed record OrderResponse(
    Guid Id,
    string OrderNumber,
    string Status,
    string Modality,
    Guid? AccountId,
    Guid CreatedByUserId,
    decimal Subtotal,
    decimal Discount,
    decimal Total,
    DateTime? ConfirmedAtUtc,
    DateTime? CompletedAtUtc,
    IReadOnlyCollection<OrderItemResponse> Items);

public sealed record CreateOrderRequest(
    string OrderNumber,
    Guid? AccountId,
    string Modality,
    IReadOnlyCollection<CreateOrderItemRequest> Items);

public sealed record CreateOrderItemRequest(
    Guid ProductId,
    int Quantity,
    IReadOnlyCollection<SelectExtraRequest> Extras,
    IReadOnlyCollection<string> RemovedIngredients);

public sealed record SelectExtraRequest(Guid ExtraId, int Quantity = 1);

public sealed record UpdateOrderItemRequest(int Quantity);

public sealed record ConfirmOrderRequest();

public sealed record OrderProductSnapshot(
    Guid Id,
    string Name,
    decimal Price,
    bool IsAvailable,
    Guid PreparationAreaId,
    IReadOnlyCollection<ProductExtra> AvailableExtras,
    IReadOnlyCollection<ProductIngredient> Ingredients);

public interface IOrderService
{
    Task<Result<OrderResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<OrderResponse>> CreateAsync(CreateOrderRequest request, Guid userId, CancellationToken cancellationToken);

    Task<Result<OrderResponse>> AddItemAsync(Guid orderId, CreateOrderItemRequest request, CancellationToken cancellationToken);

    Task<Result<OrderResponse>> UpdateItemAsync(Guid orderId, Guid itemId, UpdateOrderItemRequest request, CancellationToken cancellationToken);

    Task<Result<OrderResponse>> RemoveItemAsync(Guid orderId, Guid itemId, CancellationToken cancellationToken);

    Task<Result<OrderResponse>> ConfirmAsync(Guid orderId, CancellationToken cancellationToken);

    Task<Result> CancelAsync(Guid orderId, string? reason, CancellationToken cancellationToken);

    Task<Result> AssignStationAsync(Guid orderId, Guid stationUserId, CancellationToken cancellationToken);

    Task<Result> MarkReadyAsync(Guid orderId, CancellationToken cancellationToken);
}