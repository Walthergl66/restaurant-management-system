using Restaurant.Domain.Common;

namespace Restaurant.Domain.Catalog;

public sealed class PreparationArea : AuditableEntity
{
    public string Name { get; private set; } = string.Empty;

    public string? Description { get; private set; }

    public bool IsActive { get; private set; } = true;

    public static PreparationArea Create(string name, string? description = null)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new DomainException("El nombre del área de preparación es obligatorio.");
        }

        return new PreparationArea
        {
            Name = name.Trim(),
            Description = description,
        };
    }

    public void Update(string name, string? description, bool isActive)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new DomainException("El nombre del área de preparación es obligatorio.");
        }

        Name = name.Trim();
        Description = description;
        IsActive = isActive;
    }

    public void Activate() => IsActive = true;

    public void Deactivate() => IsActive = false;
}