using Restaurant.Domain.Common;

namespace Restaurant.Domain.Billing;

public sealed class CustomerBillingData : ValueObject
{
    public string? Identification { get; }

    public string? TaxId { get; }

    public string? LegalName { get; }

    public string? Address { get; }

    public string? Phone { get; }

    public string? Email { get; }

    public bool IsFinalConsumer { get; }

    private CustomerBillingData()
    {
    }

    private CustomerBillingData(
        string? identification,
        string? taxId,
        string? legalName,
        string? address,
        string? phone,
        string? email,
        bool isFinalConsumer)
    {
        Identification = identification;
        TaxId = taxId;
        LegalName = legalName;
        Address = address;
        Phone = phone;
        Email = email;
        IsFinalConsumer = isFinalConsumer;
    }

    public static CustomerBillingData Create(
        string? identification,
        string? taxId,
        string? legalName,
        string? address,
        string? phone,
        string? email,
        bool isFinalConsumer)
    {
        if (!isFinalConsumer && string.IsNullOrWhiteSpace(legalName))
        {
            throw new DomainException("El nombre o razón social es obligatorio para una factura con datos fiscales.");
        }

        return new CustomerBillingData(
            identification?.Trim(),
            taxId?.Trim(),
            legalName?.Trim(),
            address?.Trim(),
            phone?.Trim(),
            email?.Trim(),
            isFinalConsumer);
    }

    public static CustomerBillingData FinalConsumer() =>
        new(null, null, "Consumidor final", null, null, null, true);

    protected override IEnumerable<object?> GetEqualityComponents()
    {
        yield return Identification;
        yield return TaxId;
        yield return LegalName;
        yield return Address;
        yield return Phone;
        yield return Email;
        yield return IsFinalConsumer;
    }
}