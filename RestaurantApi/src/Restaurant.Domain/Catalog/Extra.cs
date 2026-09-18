using Restaurant.Domain.Common;

namespace Restaurant.Domain.Catalog;

public sealed class Extra : AuditableEntity
{
    public string Name { get; private set; } = string.Empty;

    public decimal Price { get; private set; }

    public bool IsActive { get; private set; } = true;

    public static Extra Create(string name, decimal price)
    {
        Validate(name, price);

        return new Extra
        {
            Name = name.Trim(),
            Price = price,
        };
    }

    public void Update(string name, decimal price, bool isActive)
    {
        Validate(name, price);

        Name = name.Trim();
        Price = price;
        IsActive = isActive;
    }

    public void Activate() => IsActive = true;

    public void Deactivate() => IsActive = false;

    private static void Validate(string name, decimal price)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new DomainException("El nombre del extra es obligatorio.");
        }

        if (price < 0)
        {
            throw new DomainException("El precio del extra no puede ser negativo.");
        }
    }
}

public sealed class ProductExtra : BaseEntity
{
    public Guid ProductId { get; private set; }

    public Guid ExtraId { get; private set; }

    public bool IsRequired { get; private set; }

    public int? MaxQuantity { get; private set; }

    public Product? Product { get; private set; }

    public Extra? Extra { get; private set; }

    public static ProductExtra Create(Guid productId, Guid extraId, bool isRequired = false, int? maxQuantity = null)
    {
        return new ProductExtra
        {
            ProductId = productId,
            ExtraId = extraId,
            IsRequired = isRequired,
            MaxQuantity = maxQuantity,
        };
    }
}

public sealed class ProductIngredient : BaseEntity
{
    public Guid ProductId { get; private set; }

    public string IngredientName { get; private set; } = string.Empty;

    public bool IsRemovable { get; private set; }

    public Product? Product { get; private set; }

    public static ProductIngredient Create(Guid productId, string ingredientName, bool isRemovable = true)
    {
        if (string.IsNullOrWhiteSpace(ingredientName))
        {
            throw new DomainException("El nombre del ingrediente es obligatorio.");
        }

        return new ProductIngredient
        {
            ProductId = productId,
            IngredientName = ingredientName.Trim(),
            IsRemovable = isRemovable,
        };
    }
}