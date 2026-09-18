using Restaurant.Application.Common;

namespace Restaurant.Application.Additions;

public sealed record AdditionItemExtraResponse(Guid ExtraId, string Name, decimal Price, int Quantity);

public sealed record AdditionItemRemovedIngredientResponse(Guid Id, string IngredientName);

public sealed record AdditionItemResponse(
    Guid Id,
    Guid ProductId,
    string ProductName,
    Guid PreparationAreaId,
    decimal UnitPrice,
    int Quantity,
    decimal LineTotal,
    IReadOnlyCollection<AdditionItemExtraResponse> Extras,
    IReadOnlyCollection<AdditionItemRemovedIngredientResponse> RemovedIngredients);

public sealed record AdditionResponse(
    Guid Id,
    Guid AccountId,
    Guid CreatedByUserId,
    string Status,
    decimal Subtotal,
    decimal Total,
    DateTime? ConfirmedAtUtc,
    IReadOnlyCollection<AdditionItemResponse> Items);

public sealed record CreateAdditionRequest(Guid AccountId, IReadOnlyCollection<CreateAdditionItemRequest> Items);

public sealed record CreateAdditionItemRequest(
    Guid ProductId,
    int Quantity,
    IReadOnlyCollection<SelectExtraRequest> Extras,
    IReadOnlyCollection<string> RemovedIngredients);

public sealed record SelectExtraRequest(Guid ExtraId, int Quantity = 1);

public interface IAdditionService
{
    Task<Result<AdditionResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<AdditionResponse>> CreateAsync(CreateAdditionRequest request, Guid userId, CancellationToken cancellationToken);

    Task<Result<AdditionResponse>> AddItemAsync(Guid additionId, CreateAdditionItemRequest request, CancellationToken cancellationToken);

    Task<Result<AdditionResponse>> RemoveItemAsync(Guid additionId, Guid itemId, CancellationToken cancellationToken);

    Task<Result<AdditionResponse>> ConfirmAsync(Guid additionId, CancellationToken cancellationToken);
}