using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Common;
using Restaurant.Domain.Orders;
using Restaurant.Domain.Preparation;

namespace Restaurant.Application.Preparation;

public sealed class PreparationOrderService(IPreparationOrderRepository repository) : IPreparationOrderService
{
    public async Task<Result<IReadOnlyCollection<PreparationOrderResponse>>> GetByStatusAsync(int status, CancellationToken cancellationToken)
    {
        if (!Enum.IsDefined(typeof(PreparationOrderStatus), status))
        {
            return Result<IReadOnlyCollection<PreparationOrderResponse>>.ValidationFailure(
                "preparation.invalid_status",
                "El estado indicado no es válido.");
        }

        var orders = await repository.GetByStatusAsync((PreparationOrderStatus)status, cancellationToken);

        return orders
            .Select(ToResponse)
            .ToList();
    }

    public async Task<Result<PreparationOrderResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var order = await repository.GetWithItemsAsync(id, cancellationToken);

        return order is null
            ? Result<PreparationOrderResponse>.NotFound("preparation.not_found", "La comanda no existe.")
            : ToResponse(order);
    }

    public async Task<Result<IReadOnlyCollection<PreparationOrderResponse>>> GenerateAsync(Order order, CancellationToken cancellationToken)
    {
        var existing = await repository.GetBySourceOrderAsync(order.Id, cancellationToken);
        if (existing.Count > 0)
        {
            return existing.Select(ToResponse).ToList();
        }

        var groups = order.Items
            .GroupBy(i => i.PreparationAreaId)
            .Select(g => new
            {
                AreaId = g.Key,
                Items = g.Select(i => PreparationOrderItem.Create(
                    i.Id,
                    i.ProductId,
                    i.ProductName,
                    i.Quantity,
                    BuildNotes(i))).ToList(),
            })
            .ToList();

        if (groups.Count == 0)
        {
            return Result<IReadOnlyCollection<PreparationOrderResponse>>.BusinessRuleFailure(
                "preparation.no_items",
                "El pedido no tiene items para preparar.");
        }

        try
        {
            var orders = new List<PreparationOrder>();
            foreach (var group in groups)
            {
                orders.Add(PreparationOrder.Create(
                    order.Id,
                    group.AreaId,
                    $"Área {group.AreaId}",
                    group.Items));
            }

            await repository.AddRangeAsync(orders, cancellationToken);
            await repository.SaveChangesAsync(cancellationToken);

            return orders.Select(ToResponse).ToList();
        }
        catch (DomainException exception)
        {
            return Result<IReadOnlyCollection<PreparationOrderResponse>>.ValidationFailure("preparation.invalid", exception.Message);
        }
    }

    public async Task<Result> StartAsync(Guid id, CancellationToken cancellationToken)
    {
        var order = await repository.GetByIdAsync(id, cancellationToken);
        if (order is null)
        {
            return Result.NotFound("preparation.not_found", "La comanda no existe.");
        }

        try
        {
            order.StartPreparation();
            await repository.SaveChangesAsync(cancellationToken);

            return Result.Success();
        }
        catch (DomainException exception)
        {
            return Result.BusinessRuleFailure("preparation.invalid", exception.Message);
        }
    }

    public async Task<Result> MarkReadyAsync(Guid id, CancellationToken cancellationToken)
    {
        var order = await repository.GetByIdAsync(id, cancellationToken);
        if (order is null)
        {
            return Result.NotFound("preparation.not_found", "La comanda no existe.");
        }

        order.MarkReady();
        await repository.SaveChangesAsync(cancellationToken);

        return Result.Success();
    }

    public async Task<Result> CancelAsync(Guid id, CancellationToken cancellationToken)
    {
        var order = await repository.GetByIdAsync(id, cancellationToken);
        if (order is null)
        {
            return Result.NotFound("preparation.not_found", "La comanda no existe.");
        }

        order.Cancel();
        await repository.SaveChangesAsync(cancellationToken);

        return Result.Success();
    }

    private static string? BuildNotes(OrderItem item)
    {
        var notes = new List<string>();

        foreach (var ingredient in item.RemovedIngredients)
        {
            notes.Add($"Sin {ingredient.IngredientName}");
        }

        foreach (var extra in item.Extras)
        {
            notes.Add(extra.Quantity > 1 ? $"+ {extra.Quantity} x {extra.Name}" : $"+ {extra.Name}");
        }

        return notes.Count == 0 ? null : string.Join(", ", notes);
    }

    private static PreparationOrderResponse ToResponse(PreparationOrder order) => new(
        order.Id,
        order.SourceOrderId,
        order.PreparationAreaId,
        order.PreparationAreaName,
        order.Status.ToString(),
        order.PrintedAtUtc,
        order.StartedAtUtc,
        order.ReadyAtUtc,
        order.Items
            .Select(i => new PreparationOrderItemResponse(
                i.Id,
                i.SourceOrderItemId,
                i.ProductId,
                i.ProductName,
                i.Quantity,
                i.Notes))
            .ToList());
}