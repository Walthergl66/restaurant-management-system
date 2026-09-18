using Restaurant.Domain.Common;

namespace Restaurant.Domain.Additions;

public enum AdditionStatus
{
    DRAFT,
    CONFIRMED,
    CANCELLED,
}

public sealed record AdditionCreatedEvent(Guid AdditionId, Guid AccountId) : DomainEvent;

public sealed record AdditionConfirmedEvent(Guid AdditionId, Guid AccountId, decimal Total) : DomainEvent;

public sealed class Addition : AggregateRoot
{
    private readonly List<AdditionItem> _items = [];

    public Guid AccountId { get; private set; }

    public Guid CreatedByUserId { get; private set; }

    public AdditionStatus Status { get; private set; } = AdditionStatus.DRAFT;

    public decimal Subtotal { get; private set; }

    public decimal Total { get; private set; }

    public DateTime? ConfirmedAtUtc { get; private set; }

    public IReadOnlyCollection<AdditionItem> Items => _items.AsReadOnly();

    public static Addition Create(Guid accountId, Guid createdByUserId)
    {
        return new Addition
        {
            AccountId = accountId,
            CreatedByUserId = createdByUserId,
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

        var item = AdditionItem.Create(
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
    }

    public void RemoveItem(Guid itemId)
    {
        EnsureDraft();

        var item = _items.FirstOrDefault(i => i.Id == itemId)
            ?? throw new DomainException("El item no pertenece a la adición.");

        _items.Remove(item);
        Recalculate();
    }

    public void Recalculate()
    {
        Subtotal = _items.Sum(i => i.LineTotal);
        Total = Subtotal;
    }

    public void Confirm()
    {
        EnsureDraft();

        if (_items.Count == 0)
        {
            throw new DomainException("No se puede confirmar una adición sin items.");
        }

        Status = AdditionStatus.CONFIRMED;
        ConfirmedAtUtc = DateTime.UtcNow;
        AddDomainEvent(new AdditionConfirmedEvent(Id, AccountId, Total));
    }

    public void Cancel()
    {
        Status = AdditionStatus.CANCELLED;
    }

    private void EnsureDraft()
    {
        if (Status != AdditionStatus.DRAFT)
        {
            throw new DomainException("Solo se pueden modificar adiciones en estado DRAFT.");
        }
    }
}

public sealed class AdditionItem : BaseEntity
{
    private readonly List<AdditionItemExtra> _extras = [];
    private readonly List<AdditionItemRemovedIngredient> _removedIngredients = [];

    public Guid AdditionId { get; private set; }

    public Guid ProductId { get; private set; }

    public string ProductName { get; private set; } = string.Empty;

    public Guid PreparationAreaId { get; private set; }

    public decimal UnitPrice { get; private set; }

    public int Quantity { get; private set; }

    public decimal LineTotal { get; private set; }

    public IReadOnlyCollection<AdditionItemExtra> Extras => _extras.AsReadOnly();

    public IReadOnlyCollection<AdditionItemRemovedIngredient> RemovedIngredients => _removedIngredients.AsReadOnly();

    public static AdditionItem Create(
        Guid additionId,
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

        var item = new AdditionItem
        {
            AdditionId = additionId,
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

                item._extras.Add(AdditionItemExtra.Create(additionId, extra.ExtraId, extra.Name, extra.Price, extra.Quantity));
            }
        }

        if (removedIngredients is not null)
        {
            foreach (var ingredient in removedIngredients)
            {
                item._removedIngredients.Add(AdditionItemRemovedIngredient.Create(additionId, ingredient.Trim()));
            }
        }

        item.RecalculateLineTotal();

        return item;
    }

    private void RecalculateLineTotal()
    {
        var baseAmount = UnitPrice * Quantity;
        var extrasAmount = _extras.Sum(e => e.Price * e.Quantity);
        LineTotal = baseAmount + extrasAmount;
    }
}

public sealed class AdditionItemExtra : BaseEntity
{
    public Guid AdditionId { get; private set; }

    public Guid ExtraId { get; private set; }

    public string Name { get; private set; } = string.Empty;

    public decimal Price { get; private set; }

    public int Quantity { get; private set; }

    public static AdditionItemExtra Create(Guid additionId, Guid extraId, string name, decimal price, int quantity = 1)
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

        return new AdditionItemExtra
        {
            AdditionId = additionId,
            ExtraId = extraId,
            Name = name.Trim(),
            Price = price,
            Quantity = quantity,
        };
    }
}

public sealed class AdditionItemRemovedIngredient : BaseEntity
{
    public Guid AdditionId { get; private set; }

    public string IngredientName { get; private set; } = string.Empty;

    public static AdditionItemRemovedIngredient Create(Guid additionId, string ingredientName)
    {
        if (string.IsNullOrWhiteSpace(ingredientName))
        {
            throw new DomainException("El nombre del ingrediente es obligatorio.");
        }

        return new AdditionItemRemovedIngredient
        {
            AdditionId = additionId,
            IngredientName = ingredientName,
        };
    }
}