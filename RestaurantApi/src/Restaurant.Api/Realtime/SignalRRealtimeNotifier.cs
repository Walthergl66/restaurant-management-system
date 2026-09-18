using Microsoft.AspNetCore.SignalR;
using Restaurant.Api.Hubs;
using Restaurant.Application.Realtime;

namespace Restaurant.Api.Realtime;

public sealed class SignalRRealtimeNotifier(IHubContext<RestaurantHub> hubContext) : IRealtimeNotifier
{
    public Task NotifyOrderCreatedAsync(Guid orderId, CancellationToken cancellationToken = default)
    {
        return hubContext.Clients.All.SendAsync("OrderCreated", orderId, cancellationToken);
    }

    public Task NotifyOrderConfirmedAsync(Guid orderId, CancellationToken cancellationToken = default)
    {
        return hubContext.Clients.All.SendAsync("OrderConfirmed", orderId, cancellationToken);
    }

    public Task NotifyOrderStatusChangedAsync(Guid orderId, string status, CancellationToken cancellationToken = default)
    {
        return hubContext.Clients.All.SendAsync("OrderStatusChanged", orderId, status, cancellationToken);
    }

    public Task NotifyPreparationOrderCreatedAsync(Guid preparationOrderId, CancellationToken cancellationToken = default)
    {
        return hubContext.Clients.All.SendAsync("PreparationOrderCreated", preparationOrderId, cancellationToken);
    }

    public Task NotifyPreparationStartedAsync(Guid preparationOrderId, CancellationToken cancellationToken = default)
    {
        return hubContext.Clients.All.SendAsync("PreparationStarted", preparationOrderId, cancellationToken);
    }

    public Task NotifyOrderReadyAsync(Guid orderId, CancellationToken cancellationToken = default)
    {
        return hubContext.Clients.All.SendAsync("OrderReady", orderId, cancellationToken);
    }

    public Task NotifyCancellationApprovedAsync(Guid requestId, CancellationToken cancellationToken = default)
    {
        return hubContext.Clients.All.SendAsync("CancellationApproved", requestId, cancellationToken);
    }
}