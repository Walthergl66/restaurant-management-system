using Restaurant.Domain.Common;

namespace Restaurant.Domain.Customers;

public sealed class Address
{
    public Guid Id { get; private set; } = Guid.NewGuid();

    public Guid CustomerProfileId { get; private set; }

    public string Label { get; private set; } = string.Empty;

    public string Line1 { get; private set; } = string.Empty;

    public string? Line2 { get; private set; }

    public string City { get; private set; } = string.Empty;

    public string? Reference { get; private set; }

    public bool IsDefault { get; private set; }

    internal static Address Create(
        Guid customerProfileId,
        string label,
        string line1,
        string? line2,
        string city,
        string? reference,
        bool isDefault)
    {
        if (string.IsNullOrWhiteSpace(line1))
        {
            throw new DomainException("La dirección es obligatoria.");
        }

        if (string.IsNullOrWhiteSpace(city))
        {
            throw new DomainException("La ciudad es obligatoria.");
        }

        return new Address
        {
            CustomerProfileId = customerProfileId,
            Label = string.IsNullOrWhiteSpace(label) ? "Principal" : label.Trim(),
            Line1 = line1.Trim(),
            Line2 = line2?.Trim(),
            City = city.Trim(),
            Reference = reference?.Trim(),
            IsDefault = isDefault,
        };
    }

    internal void MarkDefault() => IsDefault = true;

    internal void ClearDefault() => IsDefault = false;
}

public sealed class CustomerProfile : AggregateRoot
{
    public Guid? UserId { get; private set; }

    public string FullName { get; private set; } = string.Empty;

    public string? Phone { get; private set; }

    public string? Email { get; private set; }

    public bool IsActive { get; private set; } = true;

    public IReadOnlyCollection<Address> Addresses => _addresses.AsReadOnly();

    private readonly List<Address> _addresses = [];

    public static CustomerProfile Create(Guid? userId, string fullName, string? phone, string? email)
    {
        if (string.IsNullOrWhiteSpace(fullName))
        {
            throw new DomainException("El nombre del cliente es obligatorio.");
        }

        return new CustomerProfile
        {
            UserId = userId,
            FullName = fullName.Trim(),
            Phone = phone?.Trim(),
            Email = email?.Trim(),
        };
    }

    public void Update(string fullName, string? phone, string? email, bool isActive)
    {
        if (string.IsNullOrWhiteSpace(fullName))
        {
            throw new DomainException("El nombre del cliente es obligatorio.");
        }

        FullName = fullName.Trim();
        Phone = phone?.Trim();
        Email = email?.Trim();
        IsActive = isActive;
    }

    public Address AddAddress(string label, string line1, string? line2, string city, string? reference, bool isDefault)
    {
        if (isDefault)
        {
            foreach (var existing in _addresses)
            {
                existing.ClearDefault();
            }
        }

        var address = Address.Create(Id, label, line1, line2, city, reference, isDefault || _addresses.Count == 0);
        _addresses.Add(address);

        return address;
    }

    public void RemoveAddress(Guid addressId)
    {
        var address = _addresses.FirstOrDefault(a => a.Id == addressId)
            ?? throw new DomainException("La dirección no pertenece a este cliente.");

        _addresses.Remove(address);
    }

    public Address? GetDefaultAddress() => _addresses.FirstOrDefault(a => a.IsDefault) ?? _addresses.FirstOrDefault();
}