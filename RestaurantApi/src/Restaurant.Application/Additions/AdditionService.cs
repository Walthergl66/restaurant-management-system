using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Application.Orders;
using Restaurant.Domain.Additions;
using Restaurant.Domain.Common;

namespace Restaurant.Application.Additions;

public sealed class AdditionService(
    IAdditionRepository additionRepository,
    IProductRepository productRepository,
    ITableAccountRepository accountRepository) : IAdditionService
{
    public async Task<Result<AdditionResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var addition = await additionRepository.GetWithItemsAsync(id, cancellationToken);

        return addition is null
            ? Result<AdditionResponse>.NotFound("addition.not_found", "La adición no existe.")
            : ToResponse(addition);
    }

    public async Task<Result<AdditionResponse>> CreateAsync(CreateAdditionRequest request, Guid userId, CancellationToken cancellationToken)
    {
        var account = await accountRepository.GetByIdAsync(request.AccountId, cancellationToken);
        if (account is null)
        {
            return Result<AdditionResponse>.NotFound("account.not_found", "La cuenta indicada no existe.");
        }

        try
        {
            var addition = Addition.Create(request.AccountId, userId);
            if (request.Items is not null)
            {
                foreach (var item in request.Items)
                {
                    var product = await LoadProductSnapshotAsync(item.ProductId, cancellationToken);
                    if (product is null)
                    {
                        throw new DomainException($"El producto {item.ProductId} no existe.");
                    }

                    if (!product.IsAvailable)
                    {
                        throw new DomainException($"El producto '{product.Name}' no está disponible.");
                    }

                    addition.AddItem(
                        product.Id,
                        product.Name,
                        product.PreparationAreaId,
                        product.Price,
                        item.Quantity,
                        BuildExtras(product, item.Extras),
                        item.RemovedIngredients);
                }
            }

            await additionRepository.AddAsync(addition, cancellationToken);
            await additionRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(addition);
        }
        catch (DomainException exception)
        {
            return Result<AdditionResponse>.ValidationFailure("addition.invalid", exception.Message);
        }
    }

    public async Task<Result<AdditionResponse>> AddItemAsync(Guid additionId, CreateAdditionItemRequest request, CancellationToken cancellationToken)
    {
        var addition = await additionRepository.GetWithItemsAsync(additionId, cancellationToken);
        if (addition is null)
        {
            return Result<AdditionResponse>.NotFound("addition.not_found", "La adición no existe.");
        }

        var product = await LoadProductSnapshotAsync(request.ProductId, cancellationToken);
        if (product is null)
        {
            return Result<AdditionResponse>.ValidationFailure("addition.product_not_found", "El producto no existe.");
        }

        if (!product.IsAvailable)
        {
            return Result<AdditionResponse>.ValidationFailure("addition.product_unavailable", $"El producto '{product.Name}' no está disponible.");
        }

        try
        {
            addition.AddItem(
                product.Id,
                product.Name,
                product.PreparationAreaId,
                product.Price,
                request.Quantity,
                BuildExtras(product, request.Extras),
                request.RemovedIngredients);

            await additionRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(addition);
        }
        catch (DomainException exception)
        {
            return Result<AdditionResponse>.ValidationFailure("addition.invalid", exception.Message);
        }
    }

    public async Task<Result<AdditionResponse>> RemoveItemAsync(Guid additionId, Guid itemId, CancellationToken cancellationToken)
    {
        var addition = await additionRepository.GetWithItemsAsync(additionId, cancellationToken);
        if (addition is null)
        {
            return Result<AdditionResponse>.NotFound("addition.not_found", "La adición no existe.");
        }

        try
        {
            addition.RemoveItem(itemId);
            await additionRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(addition);
        }
        catch (DomainException exception)
        {
            return Result<AdditionResponse>.ValidationFailure("addition.invalid", exception.Message);
        }
    }

    public async Task<Result<AdditionResponse>> ConfirmAsync(Guid additionId, CancellationToken cancellationToken)
    {
        var addition = await additionRepository.GetWithItemsAsync(additionId, cancellationToken);
        if (addition is null)
        {
            return Result<AdditionResponse>.NotFound("addition.not_found", "La adición no existe.");
        }

        var account = await accountRepository.GetByIdAsync(addition.AccountId, cancellationToken);
        if (account is null)
        {
            return Result<AdditionResponse>.NotFound("account.not_found", "La cuenta asociada no existe.");
        }

        foreach (var item in addition.Items)
        {
            var product = await LoadProductSnapshotAsync(item.ProductId, cancellationToken);
            if (product is null)
            {
                return Result<AdditionResponse>.ValidationFailure("addition.product_invalid", $"El producto '{item.ProductName}' ya no existe.");
            }

            if (!product.IsAvailable)
            {
                return Result<AdditionResponse>.ValidationFailure("addition.product_unavailable", $"El producto '{item.ProductName}' no está disponible.");
            }
        }

        try
        {
            addition.Recalculate();
            addition.Confirm();
            account.UpdateTotals(
                account.Subtotal + addition.Total,
                account.Discount,
                account.Tip);
            await additionRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(addition);
        }
        catch (DomainException exception)
        {
            return Result<AdditionResponse>.BusinessRuleFailure("addition.invalid", exception.Message);
        }
    }

    private async Task<OrderProductSnapshot?> LoadProductSnapshotAsync(Guid productId, CancellationToken cancellationToken)
    {
        var product = await productRepository.GetWithDetailsAsync(productId, cancellationToken);
        if (product is null)
        {
            return null;
        }

        return new OrderProductSnapshot(
            product.Id,
            product.Name,
            product.GetCurrentPrice(),
            product.IsAvailable,
            product.PreparationAreaId,
            product.AvailableExtras,
            product.Ingredients);
    }

    private static List<(Guid ExtraId, string Name, decimal Price, int Quantity)>? BuildExtras(
        OrderProductSnapshot product,
        IReadOnlyCollection<SelectExtraRequest>? extras)
    {
        if (extras is null || extras.Count == 0)
        {
            return null;
        }

        var available = product.AvailableExtras
            .Where(e => e.Extra is not null)
            .ToDictionary(e => e.ExtraId);

        var result = new List<(Guid ExtraId, string Name, decimal Price, int Quantity)>();
        foreach (var extra in extras)
        {
            if (!available.TryGetValue(extra.ExtraId, out var productExtra))
            {
                throw new DomainException($"El extra {extra.ExtraId} no está disponible para el producto '{product.Name}'.");
            }

            result.Add((extra.ExtraId, productExtra.Extra!.Name, productExtra.Extra.Price, extra.Quantity));
        }

        return result;
    }

    private static AdditionResponse ToResponse(Addition addition) => new(
        addition.Id,
        addition.AccountId,
        addition.CreatedByUserId,
        addition.Status.ToString(),
        addition.Subtotal,
        addition.Total,
        addition.ConfirmedAtUtc,
        addition.Items
            .Select(i => new AdditionItemResponse(
                i.Id,
                i.ProductId,
                i.ProductName,
                i.PreparationAreaId,
                i.UnitPrice,
                i.Quantity,
                i.LineTotal,
                i.Extras
                    .Select(e => new AdditionItemExtraResponse(e.ExtraId, e.Name, e.Price, e.Quantity))
                    .ToList(),
                i.RemovedIngredients
                    .Select(r => new AdditionItemRemovedIngredientResponse(r.Id, r.IngredientName))
                    .ToList()))
            .ToList());
}