using Restaurant.Domain.Common;

namespace Restaurant.Domain.Tables;

public enum AccountStatus
{
    OPEN,
    PAYMENT_PENDING,
    PAID,
    CLOSED,
}

public sealed class TableAccount : AuditableEntity
{
    public string AccountNumber { get; private set; } = string.Empty;

    public Guid? TableId { get; private set; }

    public AccountStatus Status { get; private set; } = AccountStatus.OPEN;

    public decimal Subtotal { get; private set; }

    public decimal Discount { get; private set; }

    public decimal Tip { get; private set; }

    public decimal Total => Subtotal - Discount + Tip;

    public decimal AmountDue => Status == AccountStatus.PAID || Status == AccountStatus.CLOSED ? 0 : Total;

    public decimal PaidAmount { get; private set; }

    public DateTime? OpenedAt { get; private set; }

    public DateTime? ClosedAt { get; private set; }

    public static TableAccount Create(string accountNumber, Guid? tableId = null)
    {
        if (string.IsNullOrWhiteSpace(accountNumber))
        {
            throw new DomainException("El número de cuenta es obligatorio.");
        }

        return new TableAccount
        {
            AccountNumber = accountNumber.Trim(),
            TableId = tableId,
            OpenedAt = DateTime.UtcNow,
        };
    }

    public void UpdateTotals(decimal subtotal, decimal discount, decimal tip)
    {
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

        Subtotal = subtotal;
        Discount = discount;
        Tip = tip;
    }

    public void RegisterPayment(decimal amount)
    {
        if (amount <= 0)
        {
            throw new DomainException("El monto del pago debe ser mayor a cero.");
        }

        if (Status == AccountStatus.CLOSED)
        {
            throw new DomainException("No se puede registrar un pago en una cuenta cerrada.");
        }

        var newPaid = PaidAmount + amount;
        if (newPaid > Total + 0.01m)
        {
            throw new DomainException("El total pagado supera el monto adeudado de la cuenta.");
        }

        PaidAmount = newPaid;

        if (Status == AccountStatus.OPEN && newPaid >= Total - 0.01m)
        {
            Status = AccountStatus.PAYMENT_PENDING;
        }
        else if (Math.Abs(newPaid - Total) <= 0.01m)
        {
            Close();
        }
    }

    public void ApplyTip(decimal tip)
    {
        if (tip < 0)
        {
            throw new DomainException("La propina no puede ser negativa.");
        }

        Tip = tip;
    }

    public void RequestPayment() => Status = AccountStatus.PAYMENT_PENDING;

    public void Close()
    {
        if (Status == AccountStatus.PAID || Status == AccountStatus.CLOSED)
        {
            throw new DomainException("La cuenta ya está cerrada.");
        }

        Status = AccountStatus.CLOSED;
        ClosedAt = DateTime.UtcNow;
    }
}