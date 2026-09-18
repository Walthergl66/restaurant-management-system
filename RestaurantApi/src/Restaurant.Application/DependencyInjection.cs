using Microsoft.Extensions.DependencyInjection;
using Restaurant.Application.Auth;
using Restaurant.Application.Catalog;

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

        return services;
    }
}