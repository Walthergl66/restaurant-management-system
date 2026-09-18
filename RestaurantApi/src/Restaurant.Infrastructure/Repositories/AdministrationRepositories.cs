using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Users;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class EmployeeRepository(ApplicationDbContext dbContext) : IEmployeeRepository
{
    public async Task<IReadOnlyCollection<Employee>> GetAllAsync(CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Employee>()
            .AsNoTracking()
            .OrderBy(e => e.LastName)
            .ThenBy(e => e.FirstName)
            .ToListAsync(cancellationToken);
    }

    public async Task<Employee?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Employee>().FindAsync([id], cancellationToken);
    }

    public async Task<Employee?> GetByUserIdAsync(Guid userId, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Employee>()
            .FirstOrDefaultAsync(e => e.UserId == userId, cancellationToken);
    }

    public async Task AddAsync(Employee employee, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<Employee>().AddAsync(employee, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}

public sealed class RoleRepository(ApplicationDbContext dbContext) : IRoleRepository
{
    public async Task<IReadOnlyCollection<Role>> GetAllWithPermissionsAsync(CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Role>()
            .AsNoTracking()
            .Include(r => r.RolePermissions)
            .ThenInclude(rp => rp.Permission)
            .OrderBy(r => r.Name)
            .ToListAsync(cancellationToken);
    }

    public async Task<Role?> GetByNameAsync(string name, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Role>()
            .Include(r => r.RolePermissions)
            .ThenInclude(rp => rp.Permission)
            .FirstOrDefaultAsync(r => r.Name == name, cancellationToken);
    }

    public async Task<IReadOnlyCollection<Permission>> GetPermissionsAsync(CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Permission>()
            .AsNoTracking()
            .OrderBy(p => p.Name)
            .ToListAsync(cancellationToken);
    }

    public async Task<Permission?> GetPermissionByNameAsync(string name, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Permission>()
            .FirstOrDefaultAsync(p => p.Name == name, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}