namespace Restaurant.Application.Realtime;

public interface IRealtimeNotifier
{
    Task NotifyOrderCreatedAsync(Guid orderId, CancellationToken cancellationToken = default);

    Task NotifyOrderConfirmedAsync(Guid orderId, CancellationToken cancellationToken = default);

    Task NotifyOrderStatusChangedAsync(Guid orderId, string status, CancellationToken cancellationToken = default);

    Task NotifyPreparationOrderCreatedAsync(Guid preparationOrderId, CancellationToken cancellationToken = default);

    Task NotifyPreparationStartedAsync(Guid preparationOrderId, CancellationToken cancellationToken = default);

    Task NotifyOrderReadyAsync(Guid orderId, CancellationToken cancellationToken = default);

    Task NotifyCancellationApprovedAsync(Guid requestId, CancellationToken cancellationToken = default);
}