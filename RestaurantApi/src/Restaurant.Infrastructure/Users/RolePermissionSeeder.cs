using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Users;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Users;

public sealed class RolePermissionSeeder(
    ApplicationDbContext dbContext,
    IPasswordHasher passwordHasher,
    IConfiguration configuration) : IRolePermissionSeeder
{
    public async Task SeedAsync(CancellationToken cancellationToken = default)
    {
        await SeedPermissionsAsync(cancellationToken);
        await SeedRolesAsync(cancellationToken);
        await SeedAdminUserAsync(cancellationToken);
        await dbContext.SaveChangesAsync(cancellationToken);
    }

    private async Task SeedPermissionsAsync(CancellationToken cancellationToken)
    {
        var existing = await dbContext.Set<Permission>().Select(p => p.Name).ToListAsync(cancellationToken);

        foreach (var name in Permissions.All)
        {
            if (existing.Contains(name))
            {
                continue;
            }

            dbContext.Set<Permission>().Add(Permission.Create(name));
        }
    }

    private async Task SeedRolesAsync(CancellationToken cancellationToken)
    {
        var permissions = await dbContext.Set<Permission>().ToDictionaryAsync(p => p.Name, cancellationToken);
        var roles = await dbContext.Set<Role>().Include(r => r.RolePermissions).ToListAsync(cancellationToken);

        foreach (var roleName in Enum.GetValues<RoleName>())
        {
            var role = roles.FirstOrDefault(r => r.Name == roleName.ToString());
            if (role is null)
            {
                role = Role.Create(roleName);
                dbContext.Set<Role>().Add(role);
            }

            foreach (var permission in GetRolePermissions(roleName)
                .Select(name => permissions.GetValueOrDefault(name))
                .Where(static permission => permission is not null))
            {
                role.GrantPermission(permission!);
            }
        }
    }

    private async Task SeedAdminUserAsync(CancellationToken cancellationToken)
    {
        const string adminUsername = "admin";

        if (await dbContext.Set<User>().AnyAsync(u => u.Username == adminUsername, cancellationToken))
        {
            return;
        }

        var adminRole = await dbContext.Set<Role>().FirstAsync(r => r.Name == RoleName.ADMIN.ToString(), cancellationToken);
        var adminUser = User.Create(adminUsername, "admin@restaurant.local", passwordHasher.Hash(GetAdminPassword()));
        adminUser.AssignRole(adminRole);

        dbContext.Set<User>().Add(adminUser);
    }

    private string GetAdminPassword()
    {
        var password = configuration["Seed:AdminPassword"]
            ?? throw new InvalidOperationException("La variable de entorno 'Seed__AdminPassword' es obligatoria en el primer arranque.");

        return password;
    }

    private static IReadOnlyCollection<string> GetRolePermissions(RoleName roleName) => roleName switch
    {
        RoleName.CLIENT => new[]
        {
            Permissions.OrdersCreate,
            Permissions.CatalogView,
        },
        RoleName.WAITER => new[]
        {
            Permissions.OrdersCreate,
            Permissions.OrdersConfirm,
            Permissions.OrdersCancel,
            Permissions.CatalogView,
            Permissions.AdditionsCreate,
            Permissions.CancellationsRequest,
            Permissions.TableAccountsManage,
            Permissions.PaymentsCreate,
        },
        RoleName.SUPERVISOR => new[]
        {
            Permissions.OrdersCreate,
            Permissions.OrdersConfirm,
            Permissions.OrdersCancel,
            Permissions.CatalogView,
            Permissions.AdditionsCreate,
            Permissions.CancellationsRequest,
            Permissions.CancellationsApprove,
            Permissions.TableAccountsManage,
            Permissions.PaymentsCreate,
            Permissions.CashOpen,
            Permissions.CashClose,
            Permissions.ReportsView,
            Permissions.FinanceView,
        },
        RoleName.ADMIN => Permissions.All,
        _ => [],
    };
}