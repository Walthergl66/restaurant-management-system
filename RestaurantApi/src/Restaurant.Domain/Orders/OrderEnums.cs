namespace Restaurant.Domain.Orders;

public enum OrderStatus
{
    DRAFT,
    RECEIVED,
    CONFIRMED,
    IN_PREPARATION,
    READY,
    IN_ROUTE,
    DELIVERED,
    COMPLETED,
    CANCEL_REQUESTED,
    CANCELLED,
}

public enum OrderModality
{
    DINE_IN,
    PICKUP,
    DELIVERY,
}