namespace Restaurant.Application.Common.Abstractions;

public interface IRolePermissionSeeder
{
    Task SeedAsync(CancellationToken cancellationToken = default);
}