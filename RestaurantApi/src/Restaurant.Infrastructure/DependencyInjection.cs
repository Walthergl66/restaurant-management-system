using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Infrastructure.Authentication;
using Restaurant.Infrastructure.Authorization;
using Restaurant.Infrastructure.Persistence;
using Restaurant.Infrastructure.Repositories;
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

        services.AddScoped<IUserRepository, UserRepository>();
        services.AddScoped<ICategoryRepository, CategoryRepository>();
        services.AddScoped<IPreparationAreaRepository, PreparationAreaRepository>();
        services.AddScoped<IExtraRepository, ExtraRepository>();
        services.AddScoped<IProductRepository, ProductRepository>();
        services.AddScoped<IRestaurantTableRepository, RestaurantTableRepository>();
        services.AddScoped<ITableAccountRepository, TableAccountRepository>();
        services.AddScoped<IOrderRepository, OrderRepository>();
        services.AddScoped<IAdditionRepository, AdditionRepository>();
        services.AddScoped<ICancellationRequestRepository, CancellationRequestRepository>();
        services.AddScoped<IPreparationOrderRepository, PreparationOrderRepository>();
        services.AddScoped<IPrinterRepository, PrinterRepository>();
        services.AddScoped<IPrintJobRepository, PrintJobRepository>();
        services.AddScoped<IPaymentRepository, PaymentRepository>();
        services.AddScoped<ISaleRepository, SaleRepository>();

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