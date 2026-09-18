using Restaurant.Domain.Common;

namespace Restaurant.Domain.Printing;

public sealed class PrintJob : AggregateRoot
{
    public Guid? SourceOrderId { get; private set; }

    public Guid? PreparationOrderId { get; private set; }

    public Guid? PrinterId { get; private set; }

    public PrintJobStatus Status { get; private set; } = PrintJobStatus.PENDING;

    public int Attempts { get; private set; }

    public string IdempotencyKey { get; private set; } = string.Empty;

    public string? Error { get; private set; }

    public DateTime? CompletedAtUtc { get; private set; }

    public static PrintJob Create(
        Guid? sourceOrderId,
        Guid? preparationOrderId,
        Guid? printerId,
        string idempotencyKey)
    {
        if (sourceOrderId is null && preparationOrderId is null)
        {
            throw new DomainException("Un trabajo de impresión debe referenciar una comanda o un pedido.");
        }

        if (string.IsNullOrWhiteSpace(idempotencyKey))
        {
            throw new DomainException("La clave de idempotencia es obligatoria.");
        }

        return new PrintJob
        {
            SourceOrderId = sourceOrderId,
            PreparationOrderId = preparationOrderId,
            PrinterId = printerId,
            IdempotencyKey = idempotencyKey.Trim(),
        };
    }

    public void RegisterAttempt()
    {
        Attempts++;
        Status = PrintJobStatus.PENDING;
    }

    public void MarkSuccess()
    {
        Status = PrintJobStatus.SUCCESS;
        CompletedAtUtc = DateTime.UtcNow;
    }

    public void MarkFailed(string? error = null)
    {
        Status = PrintJobStatus.FAILED;
        Error = error;
        CompletedAtUtc = DateTime.UtcNow;
    }
}