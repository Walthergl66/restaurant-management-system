namespace Restaurant.Domain.Orders;

public enum OrderStatus
{
    DRAFT,
    CONFIRMED,
    IN_PREPARATION,
    READY,
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