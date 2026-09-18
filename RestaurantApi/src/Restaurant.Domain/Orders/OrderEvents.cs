using Restaurant.Domain.Common;

namespace Restaurant.Domain.Orders;

public sealed record OrderCreatedEvent(Guid OrderId, Guid? AccountId, OrderModality Modality) : DomainEvent;

public sealed record OrderItemAddedEvent(Guid OrderId, Guid ItemId, Guid ProductId, int Quantity) : DomainEvent;

public sealed record OrderItemRemovedEvent(Guid OrderId, Guid ItemId, Guid ProductId) : DomainEvent;

public sealed record OrderConfirmedEvent(
    Guid OrderId,
    Guid? AccountId,
    IReadOnlyCollection<Guid> ItemIds,
    decimal Total) : DomainEvent;

public sealed record OrderCancelledEvent(Guid OrderId, string Reason) : DomainEvent;