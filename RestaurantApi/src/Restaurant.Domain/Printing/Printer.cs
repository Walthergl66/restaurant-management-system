using Restaurant.Domain.Common;

namespace Restaurant.Domain.Printing;

public enum PrintJobStatus
{
    PENDING,
    SUCCESS,
    FAILED,
}

public sealed class Printer : AuditableEntity
{
    public string Name { get; private set; } = string.Empty;

    public string? Description { get; private set; }

    public string? IpAddress { get; private set; }

    public string? Port { get; private set; }

    public bool IsActive { get; private set; } = true;

    public List<Guid> PreparationAreaIds { get; private set; } = [];

    public static Printer Create(string name, string? description = null, string? ipAddress = null, string? port = null)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new DomainException("El nombre de la impresora es obligatorio.");
        }

        return new Printer
        {
            Name = name.Trim(),
            Description = description,
            IpAddress = ipAddress,
            Port = port,
        };
    }

    public void Update(string name, string? description, string? ipAddress, string? port, bool isActive)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new DomainException("El nombre de la impresora es obligatorio.");
        }

        Name = name.Trim();
        Description = description;
        IpAddress = ipAddress;
        Port = port;
        IsActive = isActive;
    }

    public void AssignToArea(Guid preparationAreaId)
    {
        if (preparationAreaId == Guid.Empty)
        {
            throw new DomainException("El área de preparación es obligatoria.");
        }

        if (!PreparationAreaIds.Contains(preparationAreaId))
        {
            PreparationAreaIds.Add(preparationAreaId);
        }
    }

    public void UnassignFromArea(Guid preparationAreaId) => PreparationAreaIds.Remove(preparationAreaId);

    public void Activate() => IsActive = true;

    public void Deactivate() => IsActive = false;
}