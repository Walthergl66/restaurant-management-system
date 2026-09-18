using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;

namespace Restaurant.Api.Hubs;

[Authorize]
public sealed class RestaurantHub : Hub
{
    public override async Task OnConnectedAsync()
    {
        var permission = Context.User?.FindFirst("permissions")?.Value;
        if (permission is not null)
        {
            var groupName = $"permission:{permission}";
            await Groups.AddToGroupAsync(Context.ConnectionId, groupName);
        }

        var role = Context.User?.FindFirst(System.Security.Claims.ClaimTypes.Role)?.Value;
        if (role is not null)
        {
            var roleGroup = $"role:{role}";
            await Groups.AddToGroupAsync(Context.ConnectionId, roleGroup);
        }

        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        if (Context.User?.FindFirst("permissions")?.Value is string permission)
        {
            await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"permission:{permission}");
        }

        if (Context.User?.FindFirst(System.Security.Claims.ClaimTypes.Role)?.Value is string role)
        {
            await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"role:{role}");
        }

        await base.OnDisconnectedAsync(exception);
    }
}