using Restaurant.Domain.Common;

namespace Restaurant.Domain.Users;

public sealed class Employee : AuditableEntity
{
    public Guid UserId { get; private set; }

    public string FirstName { get; private set; } = string.Empty;

    public string LastName { get; private set; } = string.Empty;

    public string? Phone { get; private set; }

    public bool IsActive { get; private set; } = true;

    public User? User { get; set; }

    public static Employee Create(Guid userId, string firstName, string lastName, string? phone = null)
    {
        if (string.IsNullOrWhiteSpace(firstName))
        {
            throw new DomainException("El nombre del empleado es obligatorio.");
        }

        if (string.IsNullOrWhiteSpace(lastName))
        {
            throw new DomainException("El apellido del empleado es obligatorio.");
        }

        return new Employee
        {
            UserId = userId,
            FirstName = firstName.Trim(),
            LastName = lastName.Trim(),
            Phone = phone,
        };
    }

    public void Update(string firstName, string lastName, string? phone, bool isActive)
    {
        if (string.IsNullOrWhiteSpace(firstName))
        {
            throw new DomainException("El nombre del empleado es obligatorio.");
        }

        if (string.IsNullOrWhiteSpace(lastName))
        {
            throw new DomainException("El apellido del empleado es obligatorio.");
        }

        FirstName = firstName.Trim();
        LastName = lastName.Trim();
        Phone = phone;
        IsActive = isActive;
    }
}