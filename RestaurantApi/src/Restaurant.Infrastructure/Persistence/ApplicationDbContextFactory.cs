using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;

namespace Restaurant.Infrastructure.Persistence;

public sealed class ApplicationDbContextFactory : IDesignTimeDbContextFactory<ApplicationDbContext>
{
    public ApplicationDbContext CreateDbContext(string[] args)
    {
        var options = new DbContextOptionsBuilder<ApplicationDbContext>()
            .UseNpgsql(
                Environment.GetEnvironmentVariable("CONNECTIONSTRINGS_RESTAURANT")
                ?? "Host=localhost;Database=restaurant;Username=restaurant;Password=restaurant",
                npgsql => npgsql.MigrationsHistoryTable("__EFMigrationsHistory", "restaurant"))
            .Options;

        return new ApplicationDbContext(options);
    }
}