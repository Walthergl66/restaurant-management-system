using Restaurant.Domain.Common;

namespace Restaurant.Domain.Cancellations;

public enum CancellationRequestStatus
{
    PENDING,
    APPROVED,
    REJECTED,
}

public sealed record CancellationRequestedEvent(Guid RequestId, Guid OrderId, Guid ItemId, int Quantity) : DomainEvent;

public sealed record CancellationApprovedEvent(Guid RequestId, Guid OrderId) : DomainEvent;

public sealed class CancellationRequest : AggregateRoot
{
    public Guid OrderId { get; private set; }

    public Guid ItemId { get; private set; }

    public Guid ProductId { get; private set; }

    public string ProductName { get; private set; } = string.Empty;

    public int Quantity { get; private set; }

    public Guid? TableId { get; private set; }

    public Guid RequestedByUserId { get; private set; }

    public string Reason { get; private set; } = string.Empty;

    public CancellationRequestStatus Status { get; private set; } = CancellationRequestStatus.PENDING;

    public Guid? ReviewedByUserId { get; private set; }

    public DateTime? ReviewedAtUtc { get; private set; }

    public string? ReviewNote { get; private set; }

    public static CancellationRequest Create(
        Guid orderId,
        Guid itemId,
        Guid productId,
        string productName,
        int quantity,
        Guid requestedByUserId,
        string reason,
        Guid? tableId = null)
    {
        if (quantity <= 0)
        {
            throw new DomainException("La cantidad a anular debe ser mayor a cero.");
        }

        if (string.IsNullOrWhiteSpace(reason))
        {
            throw new DomainException("El motivo de la anulación es obligatorio.");
        }

        return new CancellationRequest
        {
            OrderId = orderId,
            ItemId = itemId,
            ProductId = productId,
            ProductName = productName.Trim(),
            Quantity = quantity,
            TableId = tableId,
            RequestedByUserId = requestedByUserId,
            Reason = reason.Trim(),
        };
    }

    public void Approve(Guid reviewerUserId, string? note = null)
    {
        EnsurePending();

        Status = CancellationRequestStatus.APPROVED;
        ReviewedByUserId = reviewerUserId;
        ReviewedAtUtc = DateTime.UtcNow;
        ReviewNote = note;
        AddDomainEvent(new CancellationApprovedEvent(Id, OrderId));
    }

    public void Reject(Guid reviewerUserId, string? note = null)
    {
        EnsurePending();

        Status = CancellationRequestStatus.REJECTED;
        ReviewedByUserId = reviewerUserId;
        ReviewedAtUtc = DateTime.UtcNow;
        ReviewNote = note;
    }

    private void EnsurePending()
    {
        if (Status != CancellationRequestStatus.PENDING)
        {
            throw new DomainException("La solicitud de anulación ya fue revisada.");
        }
    }
}