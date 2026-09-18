using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Common;
using Restaurant.Domain.Orders;

namespace Restaurant.Application.Orders;

public sealed class OrderService(
    IOrderRepository orderRepository,
    IProductRepository productRepository,
    ITableAccountRepository accountRepository) : IOrderService
{
    public async Task<Result<OrderResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var order = await orderRepository.GetWithItemsAsync(id, cancellationToken);

        return order is null
            ? Result<OrderResponse>.NotFound("order.not_found", "El pedido no existe.")
            : ToResponse(order);
    }

    public async Task<Result<OrderResponse>> CreateAsync(CreateOrderRequest request, Guid userId, CancellationToken cancellationToken)
    {
        if (!Enum.TryParse<OrderModality>(request.Modality, true, out var modality))
        {
            return Result<OrderResponse>.ValidationFailure("order.invalid_modality", "La modalidad del pedido no es válida.");
        }

        if (modality == OrderModality.DINE_IN)
        {
            if (request.AccountId is null)
            {
                return Result<OrderResponse>.ValidationFailure("order.account_required", "Un pedido en local requiere una cuenta.");
            }

            var account = await accountRepository.GetByIdAsync(request.AccountId.Value, cancellationToken);
            if (account is null)
            {
                return Result<OrderResponse>.NotFound("account.not_found", "La cuenta indicada no existe.");
            }
        }

        try
        {
            var order = Order.Create(request.OrderNumber, userId, modality, request.AccountId);
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

                    order.AddItem(
                        product.Id,
                        product.Name,
                        product.PreparationAreaId,
                        product.Price,
                        item.Quantity,
                        BuildExtras(product, item.Extras),
                        item.RemovedIngredients);
                }
            }

            await orderRepository.AddAsync(order, cancellationToken);
            await orderRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(order);
        }
        catch (DomainException exception)
        {
            return Result<OrderResponse>.ValidationFailure("order.invalid", exception.Message);
        }
    }

    public async Task<Result<OrderResponse>> AddItemAsync(Guid orderId, CreateOrderItemRequest request, CancellationToken cancellationToken)
    {
        var order = await orderRepository.GetWithItemsAsync(orderId, cancellationToken);
        if (order is null)
        {
            return Result<OrderResponse>.NotFound("order.not_found", "El pedido no existe.");
        }

        var product = await LoadProductSnapshotAsync(request.ProductId, cancellationToken);
        if (product is null)
        {
            return Result<OrderResponse>.ValidationFailure("order.product_not_found", "El producto no existe.");
        }

        if (!product.IsAvailable)
        {
            return Result<OrderResponse>.ValidationFailure("order.product_unavailable", $"El producto '{product.Name}' no está disponible.");
        }

        try
        {
            order.AddItem(
                product.Id,
                product.Name,
                product.PreparationAreaId,
                product.Price,
                request.Quantity,
                BuildExtras(product, request.Extras),
                request.RemovedIngredients);

            await orderRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(order);
        }
        catch (DomainException exception)
        {
            return Result<OrderResponse>.ValidationFailure("order.invalid", exception.Message);
        }
    }

    public async Task<Result<OrderResponse>> UpdateItemAsync(Guid orderId, Guid itemId, UpdateOrderItemRequest request, CancellationToken cancellationToken)
    {
        var order = await orderRepository.GetWithItemsAsync(orderId, cancellationToken);
        if (order is null)
        {
            return Result<OrderResponse>.NotFound("order.not_found", "El pedido no existe.");
        }

        try
        {
            order.UpdateItemQuantity(itemId, request.Quantity);
            await orderRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(order);
        }
        catch (DomainException exception)
        {
            return Result<OrderResponse>.ValidationFailure("order.invalid", exception.Message);
        }
    }

    public async Task<Result<OrderResponse>> RemoveItemAsync(Guid orderId, Guid itemId, CancellationToken cancellationToken)
    {
        var order = await orderRepository.GetWithItemsAsync(orderId, cancellationToken);
        if (order is null)
        {
            return Result<OrderResponse>.NotFound("order.not_found", "El pedido no existe.");
        }

        try
        {
            order.RemoveItem(itemId);
            await orderRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(order);
        }
        catch (DomainException exception)
        {
            return Result<OrderResponse>.ValidationFailure("order.invalid", exception.Message);
        }
    }

    public async Task<Result<OrderResponse>> ConfirmAsync(Guid orderId, CancellationToken cancellationToken)
    {
        var order = await orderRepository.GetWithItemsAsync(orderId, cancellationToken);
        if (order is null)
        {
            return Result<OrderResponse>.NotFound("order.not_found", "El pedido no existe.");
        }

        foreach (var item in order.Items)
        {
            var product = await LoadProductSnapshotAsync(item.ProductId, cancellationToken);
            if (product is null)
            {
                return Result<OrderResponse>.ValidationFailure("order.product_invalid", $"El producto '{item.ProductName}' ya no existe.");
            }

            if (!product.IsAvailable)
            {
                return Result<OrderResponse>.ValidationFailure("order.product_unavailable", $"El producto '{item.ProductName}' no está disponible.");
            }

            if (product.Price != item.UnitPrice || item.PreparationAreaId != product.PreparationAreaId || item.ProductName != product.Name)
            {
                item.UpdateUnitPrice(product.Price);
                item.UpdateQuantity(item.Quantity);
                item.UpdateProductInfo(product.Name, product.PreparationAreaId);
            }
        }

        try
        {
            order.Recalculate();
            order.Confirm();
            await orderRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(order);
        }
        catch (DomainException exception)
        {
            return Result<OrderResponse>.BusinessRuleFailure("order.invalid", exception.Message);
        }
    }

    public async Task<Result> CancelAsync(Guid orderId, string? reason, CancellationToken cancellationToken)
    {
        var order = await orderRepository.GetByIdAsync(orderId, cancellationToken);
        if (order is null)
        {
            return Result.NotFound("order.not_found", "El pedido no existe.");
        }

        try
        {
            order.Cancel(reason);
            await orderRepository.SaveChangesAsync(cancellationToken);

            return Result.Success();
        }
        catch (DomainException exception)
        {
            return Result.BusinessRuleFailure("order.invalid", exception.Message);
        }
    }

    public async Task<Result> AssignStationAsync(Guid orderId, Guid stationUserId, CancellationToken cancellationToken)
    {
        var order = await orderRepository.GetByIdAsync(orderId, cancellationToken);
        if (order is null)
        {
            return Result.NotFound("order.not_found", "El pedido no existe.");
        }

        try
        {
            order.StartPreparation(stationUserId);
            await orderRepository.SaveChangesAsync(cancellationToken);

            return Result.Success();
        }
        catch (DomainException exception)
        {
            return Result.BusinessRuleFailure("order.invalid", exception.Message);
        }
    }

    public async Task<Result> MarkReadyAsync(Guid orderId, CancellationToken cancellationToken)
    {
        var order = await orderRepository.GetByIdAsync(orderId, cancellationToken);
        if (order is null)
        {
            return Result.NotFound("order.not_found", "El pedido no existe.");
        }

        try
        {
            order.MarkReady();
            await orderRepository.SaveChangesAsync(cancellationToken);

            return Result.Success();
        }
        catch (DomainException exception)
        {
            return Result.BusinessRuleFailure("order.invalid", exception.Message);
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

    private static OrderResponse ToResponse(Order order) => new(
        order.Id,
        order.OrderNumber,
        order.Status.ToString(),
        order.Modality.ToString(),
        order.AccountId,
        order.CreatedByUserId,
        order.Subtotal,
        order.Discount,
        order.Total,
        order.ConfirmedAtUtc,
        order.CompletedAtUtc,
        order.Items
            .Select(i => new OrderItemResponse(
                i.Id,
                i.ProductId,
                i.ProductName,
                i.PreparationAreaId,
                i.UnitPrice,
                i.Quantity,
                i.ExtrasTotal,
                i.LineTotal,
                i.Extras
                    .Select(e => new OrderItemExtraResponse(e.ExtraId, e.Name, e.Price, e.Quantity))
                    .ToList(),
                i.RemovedIngredients
                    .Select(r => new OrderItemRemovedIngredientResponse(r.Id, r.IngredientName))
                    .ToList()))
            .ToList());
}