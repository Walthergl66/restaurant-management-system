using Restaurant.Domain.Common;

namespace Restaurant.Domain.Tables;

public enum TableStatus
{
    AVAILABLE,
    OCCUPIED,
    RESERVED,
    MAINTENANCE,
}

public sealed class RestaurantTable : AuditableEntity
{
    public string Name { get; private set; } = string.Empty;

    public int Capacity { get; private set; }

    public TableStatus Status { get; private set; } = TableStatus.AVAILABLE;

    public string? Location { get; private set; }

    public Guid? CurrentAccountId { get; private set; }

    public static RestaurantTable Create(string name, int capacity, string? location = null)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new DomainException("El nombre de la mesa es obligatorio.");
        }

        if (capacity <= 0)
        {
            throw new DomainException("La capacidad de la mesa debe ser mayor a cero.");
        }

        return new RestaurantTable
        {
            Name = name.Trim(),
            Capacity = capacity,
            Location = location,
        };
    }

    public void Update(string name, int capacity, string? location)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new DomainException("El nombre de la mesa es obligatorio.");
        }

        if (capacity <= 0)
        {
            throw new DomainException("La capacidad de la mesa debe ser mayor a cero.");
        }

        Name = name.Trim();
        Capacity = capacity;
        Location = location;
    }

    public void SetStatus(TableStatus status) => Status = status;

    public void Occupy(Guid accountId)
    {
        if (Status == TableStatus.OCCUPIED)
        {
            throw new DomainException("La mesa ya está ocupada.");
        }

        CurrentAccountId = accountId;
        Status = TableStatus.OCCUPIED;
    }

    public void Free()
    {
        CurrentAccountId = null;
        Status = TableStatus.AVAILABLE;
    }

    public void MarkForMaintenance() => Status = TableStatus.MAINTENANCE;

    public void CancelMaintenance() => Status = TableStatus.AVAILABLE;
}