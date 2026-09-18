using Microsoft.Extensions.DependencyInjection;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Infrastructure.Repositories;

namespace Restaurant.Infrastructure.Authentication;

public static class AuthenticationServiceRegistration
{
    public static IServiceCollection AddAuthenticationServices(this IServiceCollection services)
    {
        services.AddScoped<IUserRepository, UserRepository>();
        services.AddScoped<IPasswordHasher, PasswordHasher>();
        services.AddScoped<IJwtTokenGenerator, JwtTokenGenerator>();

        return services;
    }
}