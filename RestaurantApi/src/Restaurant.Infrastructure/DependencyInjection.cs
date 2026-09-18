using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Infrastructure.Authentication;
using Restaurant.Infrastructure.Authorization;
using Restaurant.Infrastructure.Persistence;
using Restaurant.Infrastructure.Users;

namespace Restaurant.Infrastructure;

public static class DependencyInjection
{
    public static IServiceCollection AddInfrastructure(this IServiceCollection services, IConfiguration configuration)
    {
        services.AddPersistence(configuration);
        services.AddAuthenticationServices();
        services.AddRestaurantAuthorization();
        services.AddScoped<IRolePermissionSeeder, RolePermissionSeeder>();

        return services;
    }

    public static IServiceCollection AddPersistence(this IServiceCollection services, IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString("Restaurant")
            ?? throw new InvalidOperationException("Connection string 'Restaurant' no configurada.");

        services.AddDbContext<ApplicationDbContext>(
            options => options.UseNpgsql(
                connectionString,
                npgsql => npgsql.MigrationsHistoryTable("__EFMigrationsHistory", "restaurant")));

        return services;
    }
}