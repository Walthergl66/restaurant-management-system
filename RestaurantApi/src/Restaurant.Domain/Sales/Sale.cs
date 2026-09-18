using Restaurant.Domain.Common;

namespace Restaurant.Domain.Sales;

public sealed class SaleItem
{
    public Guid Id { get; private set; } = Guid.NewGuid();

    public Guid SaleId { get; private set; }

    public Guid ProductId { get; private set; }

    public string ProductName { get; private set; } = string.Empty;

    public int Quantity { get; private set; }

    public decimal UnitPrice { get; private set; }

    public decimal LineTotal => UnitPrice * Quantity;

    internal static SaleItem Create(Guid saleId, Guid productId, string productName, int quantity, decimal unitPrice)
    {
        if (quantity <= 0)
        {
            throw new DomainException("La cantidad debe ser mayor a cero.");
        }

        if (unitPrice < 0)
        {
            throw new DomainException("El precio unitario no puede ser negativo.");
        }

        return new SaleItem
        {
            SaleId = saleId,
            ProductId = productId,
            ProductName = productName.Trim(),
            Quantity = quantity,
            UnitPrice = unitPrice,
        };
    }
}

public sealed class Sale : AggregateRoot
{
    public Guid TableAccountId { get; private set; }

    public string SaleNumber { get; private set; } = string.Empty;

    public decimal Subtotal { get; private set; }

    public decimal Discount { get; private set; }

    public decimal Tip { get; private set; }

    public decimal Total => Subtotal - Discount + Tip;

    public decimal PaidAmount { get; private set; }

    public DateTime? PaidAtUtc { get; private set; }

    public Guid? InvoiceId { get; private set; }

    public IReadOnlyCollection<SaleItem> Items => _items.AsReadOnly();

    private readonly List<SaleItem> _items = [];

    public static Sale Create(string saleNumber, Guid tableAccountId, decimal subtotal, decimal discount, decimal tip)
    {
        if (string.IsNullOrWhiteSpace(saleNumber))
        {
            throw new DomainException("El número de venta es obligatorio.");
        }

        if (subtotal < 0)
        {
            throw new DomainException("El subtotal no puede ser negativo.");
        }

        if (discount < 0)
        {
            throw new DomainException("El descuento no puede ser negativo.");
        }

        if (tip < 0)
        {
            throw new DomainException("La propina no puede ser negativa.");
        }

        return new Sale
        {
            SaleNumber = saleNumber.Trim(),
            TableAccountId = tableAccountId,
            Subtotal = subtotal,
            Discount = discount,
            Tip = tip,
        };
    }

    public void AddItem(Guid productId, string productName, int quantity, decimal unitPrice)
    {
        _items.Add(SaleItem.Create(Id, productId, productName, quantity, unitPrice));
    }

    public void RecalculateSubtotal() => Subtotal = _items.Sum(i => i.LineTotal);

    public void RegisterPayment(decimal amount)
    {
        if (amount <= 0)
        {
            throw new DomainException("El monto del pago debe ser mayor a cero.");
        }

        var newPaid = PaidAmount + amount;
        if (newPaid > Total + 0.01m)
        {
            throw new DomainException("El total pagado supera el monto de la venta.");
        }

        PaidAmount = newPaid;
        if (Math.Abs(newPaid - Total) <= 0.01m)
        {
            PaidAtUtc = DateTime.UtcNow;
        }
    }

    public void AttachInvoice(Guid invoiceId)
    {
        if (Total > 0 && PaidAmount < Total - 0.01m)
        {
            throw new DomainException("No se puede facturar una venta no pagada.");
        }

        InvoiceId = invoiceId;
    }

    public bool IsFullyPaid => PaidAmount >= Total - 0.01m;
}