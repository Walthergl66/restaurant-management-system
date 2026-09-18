using Restaurant.Domain.Common;

namespace Restaurant.Domain.Billing;

public enum InvoiceStatus
{
    DRAFT,
    ISSUED,
    CANCELLED,
}

public sealed class InvoiceItem
{
    public Guid Id { get; private set; } = Guid.NewGuid();

    public Guid InvoiceId { get; private set; }

    public Guid ProductId { get; private set; }

    public string Description { get; private set; } = string.Empty;

    public int Quantity { get; private set; }

    public decimal UnitPrice { get; private set; }

    public decimal LineTotal => UnitPrice * Quantity;

    internal static InvoiceItem Create(Guid invoiceId, Guid productId, string description, int quantity, decimal unitPrice)
    {
        if (quantity <= 0)
        {
            throw new DomainException("La cantidad debe ser mayor a cero.");
        }

        if (unitPrice < 0)
        {
            throw new DomainException("El precio unitario no puede ser negativo.");
        }

        return new InvoiceItem
        {
            InvoiceId = invoiceId,
            ProductId = productId,
            Description = description.Trim(),
            Quantity = quantity,
            UnitPrice = unitPrice,
        };
    }
}

public sealed class Invoice : AggregateRoot
{
    public string InvoiceNumber { get; private set; } = string.Empty;

    public Guid? SaleId { get; private set; }

    public Guid? TableAccountId { get; private set; }

    public InvoiceStatus Status { get; private set; } = InvoiceStatus.DRAFT;

    public decimal Subtotal { get; private set; }

    public decimal Discount { get; private set; }

    public decimal Tip { get; private set; }

    public decimal Total => Subtotal - Discount + Tip;

    public string? AuthorizationNumber { get; private set; }

    public DateTime? IssuedAtUtc { get; private set; }

    public CustomerBillingData CustomerData { get; private set; } = null!;

    public IReadOnlyCollection<InvoiceItem> Items => _items.AsReadOnly();

    private readonly List<InvoiceItem> _items = [];

    public static Invoice Create(
        string invoiceNumber,
        CustomerBillingData customerData,
        Guid? saleId = null,
        Guid? tableAccountId = null)
    {
        if (string.IsNullOrWhiteSpace(invoiceNumber))
        {
            throw new DomainException("El número de factura es obligatorio.");
        }

        return new Invoice
        {
            InvoiceNumber = invoiceNumber.Trim(),
            CustomerData = customerData,
            SaleId = saleId,
            TableAccountId = tableAccountId,
        };
    }

    public void AddItem(Guid productId, string description, int quantity, decimal unitPrice)
    {
        if (Status != InvoiceStatus.DRAFT)
        {
            throw new DomainException("No se pueden añadir items a una factura emitida.");
        }

        _items.Add(InvoiceItem.Create(Id, productId, description, quantity, unitPrice));
    }

    public void SetTotals(decimal subtotal, decimal discount, decimal tip)
    {
        if (Status != InvoiceStatus.DRAFT)
        {
            throw new DomainException("No se pueden modificar los totales de una factura emitida.");
        }

        Subtotal = subtotal;
        Discount = discount;
        Tip = tip;
    }

    public void Issue(string? authorizationNumber = null)
    {
        if (Status != InvoiceStatus.DRAFT)
        {
            throw new DomainException("La factura ya fue emitida o anulada.");
        }

        if (_items.Count == 0 && Subtotal <= 0)
        {
            throw new DomainException("La factura no tiene items ni totales.");
        }

        Status = InvoiceStatus.ISSUED;
        AuthorizationNumber = authorizationNumber;
        IssuedAtUtc = DateTime.UtcNow;
    }

    public void Cancel()
    {
        if (Status == InvoiceStatus.CANCELLED)
        {
            throw new DomainException("La factura ya está anulada.");
        }

        Status = InvoiceStatus.CANCELLED;
    }
}