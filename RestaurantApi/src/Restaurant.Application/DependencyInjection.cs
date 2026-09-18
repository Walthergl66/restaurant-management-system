using Microsoft.Extensions.DependencyInjection;
using Restaurant.Application.Additions;
using Restaurant.Application.Auth;
using Restaurant.Application.Cancellations;
using Restaurant.Application.Catalog;
using Restaurant.Application.Orders;
using Restaurant.Application.Preparation;
using Restaurant.Application.Printing;
using Restaurant.Application.Services;
using Restaurant.Application.Tables;

namespace Restaurant.Application;

public static class DependencyInjection
{
    public static IServiceCollection AddApplication(this IServiceCollection services)
    {
        services.AddScoped<IAuthService, AuthService>();
        services.AddScoped<ICategoryService, CategoryService>();
        services.AddScoped<IPreparationAreaService, PreparationAreaService>();
        services.AddScoped<IExtraService, ExtraService>();
        services.AddScoped<IProductService, ProductService>();
        services.AddScoped<ITableService, TableService>();
        services.AddScoped<ITableAccountService, TableAccountService>();
        services.AddScoped<IOrderService, OrderService>();
        services.AddScoped<IAdditionService, AdditionService>();
        services.AddScoped<ICancellationService, CancellationService>();
        services.AddScoped<IPreparationOrderService, PreparationOrderService>();
        services.AddScoped<IPrinterService, PrinterService>();
        services.AddScoped<IPrintJobService, PrintJobService>();

        return services;
    }
}