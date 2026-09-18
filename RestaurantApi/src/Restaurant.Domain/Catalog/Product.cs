using Restaurant.Domain.Common;

namespace Restaurant.Domain.Catalog;

public sealed class Product : AuditableEntity
{
    private readonly List<ProductPrice> _prices = [];
    private readonly List<ProductExtra> _availableExtras = [];
    private readonly List<ProductIngredient> _ingredients = [];

    public string Name { get; private set; } = string.Empty;

    public string? Description { get; private set; }

    public Guid CategoryId { get; private set; }

    public Guid PreparationAreaId { get; private set; }

    public bool IsAvailable { get; private set; } = true;

    public Category? Category { get; private set; }

    public PreparationArea? PreparationArea { get; private set; }

    public IReadOnlyCollection<ProductPrice> Prices => _prices.AsReadOnly();

    public IReadOnlyCollection<ProductExtra> AvailableExtras => _availableExtras.AsReadOnly();

    public IReadOnlyCollection<ProductIngredient> Ingredients => _ingredients.AsReadOnly();

    public static Product Create(string name, Guid categoryId, Guid preparationAreaId, string? description = null, bool isAvailable = true)
    {
        ValidateName(name);

        return new Product
        {
            Name = name.Trim(),
            Description = description,
            CategoryId = categoryId,
            PreparationAreaId = preparationAreaId,
            IsAvailable = isAvailable,
        };
    }

    public void Update(string name, Guid categoryId, Guid preparationAreaId, string? description, bool isAvailable)
    {
        ValidateName(name);

        Name = name.Trim();
        Description = description;
        CategoryId = categoryId;
        PreparationAreaId = preparationAreaId;
        IsAvailable = isAvailable;
    }

    public void SetAvailability(bool isAvailable) => IsAvailable = isAvailable;

    public void SetPrice(decimal price, DateTime effectiveFrom)
    {
        if (price <= 0)
        {
            throw new DomainException("El precio del producto debe ser mayor a cero.");
        }

        if (_prices.Any(p => p.EffectiveFrom == effectiveFrom))
        {
            throw new DomainException("Ya existe un precio vigente desde esa fecha.");
        }

        _prices.Add(ProductPrice.Create(Id, price, effectiveFrom));
    }

    public decimal GetCurrentPrice() =>
        _prices
            .Where(p => p.EffectiveFrom <= DateTime.UtcNow)
            .OrderByDescending(p => p.EffectiveFrom)
            .FirstOrDefault()?.Price
        ?? throw new DomainException($"El producto '{Name}' no tiene un precio vigente.");

    public void AddExtra(ProductExtra extra)
    {
        if (extra.ProductId != Id)
        {
            throw new DomainException("El extra no pertenece a este producto.");
        }

        if (_availableExtras.Any(e => e.ExtraId == extra.ExtraId))
        {
            return;
        }

        _availableExtras.Add(extra);
    }

    public void RemoveExtra(Guid extraId)
    {
        _availableExtras.RemoveAll(e => e.ExtraId == extraId);
    }

    public void AddIngredient(ProductIngredient ingredient)
    {
        if (_ingredients.Any(i => i.IngredientName.Equals(ingredient.IngredientName, StringComparison.OrdinalIgnoreCase)))
        {
            return;
        }

        _ingredients.Add(ingredient);
    }

    public void RemoveIngredient(string ingredientName)
    {
        _ingredients.RemoveAll(i => i.IngredientName.Equals(ingredientName, StringComparison.OrdinalIgnoreCase));
    }

    private static void ValidateName(string name)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new DomainException("El nombre del producto es obligatorio.");
        }
    }
}

public sealed class ProductPrice : BaseEntity
{
    public Guid ProductId { get; private set; }

    public decimal Price { get; private set; }

    public DateTime EffectiveFrom { get; private set; }

    public Guid? UpdatedBy { get; private set; }

    public Product? Product { get; private set; }

    public static ProductPrice Create(Guid productId, decimal price, DateTime effectiveFrom)
    {
        return new ProductPrice
        {
            ProductId = productId,
            Price = price,
            EffectiveFrom = effectiveFrom,
        };
    }
}