using Restaurant.Domain.Common;

namespace Restaurant.Domain.Preparation;

public enum PreparationOrderStatus
{
    PENDING,
    PRINTED,
    IN_PREPARATION,
    READY,
    CANCELLED,
}

public sealed class PreparationOrder : AggregateRoot
{
    private readonly List<PreparationOrderItem> _items = [];

    public Guid SourceOrderId { get; private set; }

    public Guid PreparationAreaId { get; private set; }

    public string PreparationAreaName { get; private set; } = string.Empty;

    public PreparationOrderStatus Status { get; private set; } = PreparationOrderStatus.PENDING;

    public DateTime? PrintedAtUtc { get; private set; }

    public DateTime? StartedAtUtc { get; private set; }

    public DateTime? ReadyAtUtc { get; private set; }

    public IReadOnlyCollection<PreparationOrderItem> Items => _items.AsReadOnly();

    public static PreparationOrder Create(
        Guid sourceOrderId,
        Guid preparationAreaId,
        string preparationAreaName,
        IEnumerable<PreparationOrderItem> items)
    {
        if (string.IsNullOrWhiteSpace(preparationAreaName))
        {
            throw new DomainException("El nombre del área de preparación es obligatorio.");
        }

        var order = new PreparationOrder
        {
            SourceOrderId = sourceOrderId,
            PreparationAreaId = preparationAreaId,
            PreparationAreaName = preparationAreaName.Trim(),
        };

        var materializedItems = items?.ToList() ?? [];
        if (materializedItems.Count == 0)
        {
            throw new DomainException("Una comanda debe tener al menos un item.");
        }

        foreach (var item in materializedItems)
        {
            item.AttachTo(order.Id);
            order._items.Add(item);
        }

        return order;
    }

    public void MarkPrinted()
    {
        if (Status == PreparationOrderStatus.PRINTED || Status == PreparationOrderStatus.IN_PREPARATION || Status == PreparationOrderStatus.READY)
        {
            return;
        }

        Status = PreparationOrderStatus.PRINTED;
        PrintedAtUtc = DateTime.UtcNow;
    }

    public void StartPreparation()
    {
        if (Status == PreparationOrderStatus.CANCELLED)
        {
            throw new DomainException("No se puede iniciar una comanda cancelada.");
        }

        Status = PreparationOrderStatus.IN_PREPARATION;
        StartedAtUtc = DateTime.UtcNow;
    }

    public void MarkReady()
    {
        Status = PreparationOrderStatus.READY;
        ReadyAtUtc = DateTime.UtcNow;
    }

    public void Cancel()
    {
        Status = PreparationOrderStatus.CANCELLED;
    }
}

public sealed class PreparationOrderItem : BaseEntity
{
    public Guid PreparationOrderId { get; private set; }

    public Guid SourceOrderItemId { get; private set; }

    public Guid ProductId { get; private set; }

    public string ProductName { get; private set; } = string.Empty;

    public int Quantity { get; private set; }

    public string? Notes { get; private set; }

    public static PreparationOrderItem Create(
        Guid sourceOrderItemId,
        Guid productId,
        string productName,
        int quantity,
        string? notes = null)
    {
        if (string.IsNullOrWhiteSpace(productName))
        {
            throw new DomainException("El nombre del producto es obligatorio.");
        }

        if (quantity <= 0)
        {
            throw new DomainException("La cantidad debe ser mayor a cero.");
        }

        return new PreparationOrderItem
        {
            SourceOrderItemId = sourceOrderItemId,
            ProductId = productId,
            ProductName = productName.Trim(),
            Quantity = quantity,
            Notes = notes,
        };
    }

    internal void AttachTo(Guid preparationOrderId)
    {
        PreparationOrderId = preparationOrderId;
    }
}