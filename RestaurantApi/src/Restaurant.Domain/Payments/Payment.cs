using Restaurant.Domain.Common;

namespace Restaurant.Domain.Payments;

public enum PaymentMethod
{
    CASH,
    CARD,
    TRANSFER,
    OTHER,
}

public enum PaymentStatus
{
    PENDING,
    PAID,
    FAILED,
    REFUNDED,
}

public sealed class Payment : AuditableEntity
{
    public Guid? TableAccountId { get; private set; }

    public Guid? SaleId { get; private set; }

    public decimal Amount { get; private set; }

    public PaymentMethod Method { get; private set; }

    public PaymentStatus Status { get; private set; } = PaymentStatus.PENDING;

    public Guid? ProcessedBy { get; private set; }

    public DateTime? ProcessedAtUtc { get; private set; }

    public string? Reference { get; private set; }

    public static Payment Create(decimal amount, PaymentMethod method, Guid? tableAccountId = null, Guid? saleId = null, string? reference = null)
    {
        if (amount <= 0)
        {
            throw new DomainException("El monto del pago debe ser mayor a cero.");
        }

        return new Payment
        {
            TableAccountId = tableAccountId,
            SaleId = saleId,
            Amount = amount,
            Method = method,
            Reference = reference,
        };
    }

    public void MarkPaid(Guid processedBy)
    {
        if (Status == PaymentStatus.REFUNDED)
        {
            throw new DomainException("Un pago reembolsado no puede marcarse como pagado.");
        }

        Status = PaymentStatus.PAID;
        ProcessedBy = processedBy;
        ProcessedAtUtc = DateTime.UtcNow;
    }

    public void MarkFailed(string? reason = null)
    {
        Status = PaymentStatus.FAILED;
    }

    public void Refund()
    {
        if (Status != PaymentStatus.PAID)
        {
            throw new DomainException("Solo un pago pagado puede reembolsarse.");
        }

        Status = PaymentStatus.REFUNDED;
    }
}