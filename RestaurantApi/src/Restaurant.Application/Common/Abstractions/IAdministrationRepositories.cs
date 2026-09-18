using Restaurant.Domain.Users;

namespace Restaurant.Application.Common.Abstractions;

public interface IEmployeeRepository
{
    Task<IReadOnlyCollection<Employee>> GetAllAsync(CancellationToken cancellationToken = default);

    Task<Employee?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task<Employee?> GetByUserIdAsync(Guid userId, CancellationToken cancellationToken = default);

    Task AddAsync(Employee employee, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}

public interface IRoleRepository
{
    Task<IReadOnlyCollection<Role>> GetAllWithPermissionsAsync(CancellationToken cancellationToken = default);

    Task<Role?> GetByNameAsync(string name, CancellationToken cancellationToken = default);

    Task<IReadOnlyCollection<Permission>> GetPermissionsAsync(CancellationToken cancellationToken = default);

    Task<Permission?> GetPermissionByNameAsync(string name, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}