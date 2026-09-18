using Restaurant.Domain.Common;

namespace Restaurant.Domain.Catalog;

public sealed class Category : AuditableEntity
{
    private readonly List<Product> _products = [];

    public string Name { get; private set; } = string.Empty;

    public string? Description { get; private set; }

    public bool IsActive { get; private set; } = true;

    public int SortOrder { get; private set; }

    public IReadOnlyCollection<Product> Products => _products.AsReadOnly();

    public static Category Create(string name, string? description = null, int sortOrder = 0)
    {
        ValidateName(name);

        return new Category
        {
            Name = name.Trim(),
            Description = description,
            SortOrder = sortOrder,
        };
    }

    public void Update(string name, string? description, int sortOrder)
    {
        ValidateName(name);

        Name = name.Trim();
        Description = description;
        SortOrder = sortOrder;
    }

    public void Activate() => IsActive = true;

    public void Deactivate() => IsActive = false;

    private static void ValidateName(string name)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new DomainException("El nombre de la categoría es obligatorio.");
        }
    }
}