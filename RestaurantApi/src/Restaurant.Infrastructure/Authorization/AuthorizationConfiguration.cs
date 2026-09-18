using Microsoft.AspNetCore.Authorization;
using Microsoft.Extensions.DependencyInjection;
using Restaurant.Domain.Users;

namespace Restaurant.Infrastructure.Authorization;

public static class AuthorizationConfiguration
{
    public static IServiceCollection AddRestaurantAuthorization(this IServiceCollection services)
    {
        services.AddAuthorization(options =>
        {
            foreach (var permission in Permissions.All)
            {
                options.AddPolicy(
                    permission,
                    policy => policy.RequireClaim("permissions", permission));
            }
        });

        return services;
    }
}