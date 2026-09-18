using Restaurant.Domain.Common;
using Restaurant.Domain.Catalog;

namespace Restaurant.Domain.Orders;

public sealed class Order : AggregateRoot
{
    private readonly List<OrderItem> _items = [];

    public string OrderNumber { get; private set; } = string.Empty;

    public OrderStatus Status { get; private set; } = OrderStatus.DRAFT;

    public OrderModality Modality { get; private set; }

    public Guid? AccountId { get; private set; }

    public Guid CreatedByUserId { get; private set; }

    public Guid? StationUserId { get; private set; }

    public DateTime? ConfirmedAtUtc { get; private set; }

    public DateTime? CompletedAtUtc { get; private set; }

    public decimal Subtotal { get; private set; }

    public decimal Discount { get; private set; }

    public decimal Total { get; private set; }

    public IReadOnlyCollection<OrderItem> Items => _items.AsReadOnly();

    public static Order Create(string orderNumber, Guid createdByUserId, OrderModality modality, Guid? accountId = null)
    {
        if (string.IsNullOrWhiteSpace(orderNumber))
        {
            throw new DomainException("El número de pedido es obligatorio.");
        }

        if (modality == OrderModality.DINE_IN && accountId is null)
        {
            throw new DomainException("Un pedido en local debe asociarse a una cuenta.");
        }

        return new Order
        {
            OrderNumber = orderNumber.Trim(),
            CreatedByUserId = createdByUserId,
            Modality = modality,
            AccountId = accountId,
            CreatedBy = createdByUserId,
        };
    }

    public void AddItem(
        Guid productId,
        string productName,
        Guid preparationAreaId,
        decimal unitPrice,
        int quantity,
        IEnumerable<(Guid ExtraId, string Name, decimal Price, int Quantity)>? extras = null,
        IEnumerable<string>? removedIngredients = null)
    {
        EnsureDraft();

        if (string.IsNullOrWhiteSpace(productName))
        {
            throw new DomainException("El nombre del producto es obligatorio.");
        }

        if (unitPrice <= 0)
        {
            throw new DomainException("El precio unitario debe ser mayor a cero.");
        }

        if (quantity <= 0)
        {
            throw new DomainException("La cantidad debe ser mayor a cero.");
        }

        var item = OrderItem.Create(
            Id,
            productId,
            productName,
            preparationAreaId,
            unitPrice,
            quantity,
            extras,
            removedIngredients);

        _items.Add(item);
        Recalculate();
        AddDomainEvent(new OrderItemAddedEvent(Id, item.Id, productId, quantity));
    }

    public void UpdateItemQuantity(Guid itemId, int quantity)
    {
        EnsureDraft();

        if (quantity <= 0)
        {
            throw new DomainException("La cantidad debe ser mayor a cero.");
        }

        var item = _items.FirstOrDefault(i => i.Id == itemId)
            ?? throw new DomainException("El item no pertenece a este pedido.");

        item.UpdateQuantity(quantity);
        Recalculate();
    }

    public void RemoveItem(Guid itemId)
    {
        EnsureDraft();

        var item = _items.FirstOrDefault(i => i.Id == itemId)
            ?? throw new DomainException("El item no pertenece a este pedido.");

        _items.Remove(item);
        Recalculate();
        AddDomainEvent(new OrderItemRemovedEvent(Id, itemId, item.ProductId));
    }

    public void Recalculate()
    {
        Subtotal = _items.Sum(i => i.LineTotal);
        Total = Subtotal - Discount;
    }

    public void Confirm()
    {
        EnsureDraft();

        if (_items.Count == 0)
        {
            throw new DomainException("No se puede confirmar un pedido sin items.");
        }

        Status = OrderStatus.CONFIRMED;
        ConfirmedAtUtc = DateTime.UtcNow;
        AddDomainEvent(new OrderConfirmedEvent(
            Id,
            AccountId,
            _items.Select(i => i.Id).ToList(),
            Total));
    }

    public void StartPreparation(Guid stationUserId)
    {
        EnsureConfirmedOrInPreparation();
        Status = OrderStatus.IN_PREPARATION;
        StationUserId = stationUserId;
    }

    public void MarkReady()
    {
        EnsureConfirmedOrInPreparation();
        Status = OrderStatus.READY;
    }

    public void MarkDelivered()
    {
        if (Status != OrderStatus.READY && Status != OrderStatus.DELIVERED)
        {
            throw new DomainException("El pedido debe estar listo para marcarse como entregado.");
        }

        Status = OrderStatus.DELIVERED;
    }

    public void Complete()
    {
        if (Status != OrderStatus.DELIVERED)
        {
            throw new DomainException("El pedido debe estar entregado para completarse.");
        }

        Status = OrderStatus.COMPLETED;
        CompletedAtUtc = DateTime.UtcNow;
    }

    public void RequestCancellation(string? reason = null)
    {
        if (Status == OrderStatus.CANCELLED || Status == OrderStatus.COMPLETED)
        {
            throw new DomainException("El pedido ya está cerrado.");
        }

        Status = OrderStatus.CANCEL_REQUESTED;
        AddDomainEvent(new OrderCancelledEvent(Id, reason ?? string.Empty));
    }

    public void Cancel(string? reason = null)
    {
        if (Status == OrderStatus.CANCELLED || Status == OrderStatus.COMPLETED)
        {
            throw new DomainException("El pedido ya está cerrado.");
        }

        Status = OrderStatus.CANCELLED;
        AddDomainEvent(new OrderCancelledEvent(Id, reason ?? string.Empty));
    }

    public void ApplyDiscount(decimal discount)
    {
        EnsureDraft();

        if (discount < 0)
        {
            throw new DomainException("El descuento no puede ser negativo.");
        }

        if (discount > Subtotal)
        {
            throw new DomainException("El descuento no puede superar el subtotal.");
        }

        Discount = discount;
        Recalculate();
    }

    public int GetItemCount() => _items.Count;

    private void EnsureDraft()
    {
        if (Status != OrderStatus.DRAFT)
        {
            throw new DomainException("Solo se pueden modificar pedidos en estado DRAFT.");
        }
    }

    private void EnsureConfirmedOrInPreparation()
    {
        if (Status != OrderStatus.CONFIRMED && Status != OrderStatus.IN_PREPARATION)
        {
            throw new DomainException("El pedido no está en preparación.");
        }
    }
}

public sealed class OrderItem : BaseEntity
{
    private readonly List<OrderItemExtra> _extras = [];
    private readonly List<OrderItemRemovedIngredient> _removedIngredients = [];

    public Guid OrderId { get; private set; }

    public Guid ProductId { get; private set; }

    public string ProductName { get; private set; } = string.Empty;

    public Guid PreparationAreaId { get; private set; }

    public decimal UnitPrice { get; private set; }

    public int Quantity { get; private set; }

    public decimal ExtrasTotal { get; private set; }

    public decimal LineTotal { get; private set; }

    public IReadOnlyCollection<OrderItemExtra> Extras => _extras.AsReadOnly();

    public IReadOnlyCollection<OrderItemRemovedIngredient> RemovedIngredients => _removedIngredients.AsReadOnly();

    public static OrderItem Create(
        Guid orderId,
        Guid productId,
        string productName,
        Guid preparationAreaId,
        decimal unitPrice,
        int quantity,
        IEnumerable<(Guid ExtraId, string Name, decimal Price, int Quantity)>? extras = null,
        IEnumerable<string>? removedIngredients = null)
    {
        if (quantity <= 0)
        {
            throw new DomainException("La cantidad debe ser mayor a cero.");
        }

        if (unitPrice <= 0)
        {
            throw new DomainException("El precio unitario debe ser mayor a cero.");
        }

        var item = new OrderItem
        {
            OrderId = orderId,
            ProductId = productId,
            ProductName = productName,
            PreparationAreaId = preparationAreaId,
            UnitPrice = unitPrice,
            Quantity = quantity,
        };

        if (extras is not null)
        {
            foreach (var extra in extras)
            {
                if (extra.Price < 0)
                {
                    throw new DomainException("El precio de un extra no puede ser negativo.");
                }

                item._extras.Add(OrderItemExtra.Create(
                    orderId,
                    extra.ExtraId,
                    extra.Name,
                    extra.Price,
                    extra.Quantity));
            }
        }

        if (removedIngredients is not null)
        {
            foreach (var ingredient in removedIngredients)
            {
                item._removedIngredients.Add(OrderItemRemovedIngredient.Create(orderId, ingredient.Trim()));
            }
        }

        item.RecalculateLineTotal();

        return item;
    }

    public void UpdateQuantity(int quantity)
    {
        if (quantity <= 0)
        {
            throw new DomainException("La cantidad debe ser mayor a cero.");
        }

        Quantity = quantity;
        RecalculateLineTotal();
    }

    public void UpdateUnitPrice(decimal unitPrice)
    {
        if (unitPrice <= 0)
        {
            throw new DomainException("El precio unitario debe ser mayor a cero.");
        }

        UnitPrice = unitPrice;
        RecalculateLineTotal();
    }

    public void UpdateProductInfo(string productName, Guid preparationAreaId)
    {
        if (string.IsNullOrWhiteSpace(productName))
        {
            throw new DomainException("El nombre del producto es obligatorio.");
        }

        ProductName = productName.Trim();
        PreparationAreaId = preparationAreaId;
    }

    private void RecalculateLineTotal()
    {
        var baseAmount = UnitPrice * Quantity;
        ExtrasTotal = _extras.Sum(e => e.Price * e.Quantity);
        LineTotal = baseAmount + ExtrasTotal;
    }
}

public sealed class OrderItemExtra : BaseEntity
{
    public Guid OrderId { get; private set; }

    public Guid ExtraId { get; private set; }

    public string Name { get; private set; } = string.Empty;

    public decimal Price { get; private set; }

    public int Quantity { get; private set; }

    public static OrderItemExtra Create(Guid orderId, Guid extraId, string name, decimal price, int quantity = 1)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new DomainException("El nombre del extra es obligatorio.");
        }

        if (price < 0)
        {
            throw new DomainException("El precio del extra no puede ser negativo.");
        }

        if (quantity <= 0)
        {
            throw new DomainException("La cantidad del extra debe ser mayor a cero.");
        }

        return new OrderItemExtra
        {
            OrderId = orderId,
            ExtraId = extraId,
            Name = name.Trim(),
            Price = price,
            Quantity = quantity,
        };
    }
}

public sealed class OrderItemRemovedIngredient : BaseEntity
{
    public Guid OrderId { get; private set; }

    public string IngredientName { get; private set; } = string.Empty;

    public static OrderItemRemovedIngredient Create(Guid orderId, string ingredientName)
    {
        if (string.IsNullOrWhiteSpace(ingredientName))
        {
            throw new DomainException("El nombre del ingrediente es obligatorio.");
        }

        return new OrderItemRemovedIngredient
        {
            OrderId = orderId,
            IngredientName = ingredientName,
        };
    }
}