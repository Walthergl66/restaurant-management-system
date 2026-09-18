using Restaurant.Application.Administration;
using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Common;
using Restaurant.Domain.Users;

namespace Restaurant.Application.Services;

public sealed class EmployeeService(
    IEmployeeRepository employeeRepository,
    IUserRepository userRepository,
    IRoleRepository roleRepository,
    IPasswordHasher passwordHasher) : IEmployeeService
{
    public async Task<Result<IReadOnlyCollection<EmployeeResponse>>> GetAllAsync(CancellationToken cancellationToken)
    {
        var employees = await employeeRepository.GetAllAsync(cancellationToken);
        var responses = new List<EmployeeResponse>();

        foreach (var employee in employees)
        {
            var user = await userRepository.GetByIdAsync(employee.UserId, cancellationToken);
            responses.Add(ToResponse(employee, user));
        }

        return responses;
    }

    public async Task<Result<EmployeeResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var employee = await employeeRepository.GetByIdAsync(id, cancellationToken);
        if (employee is null)
        {
            return Result<EmployeeResponse>.NotFound("employee.not_found", "El empleado no existe.");
        }

        var user = await userRepository.GetByIdAsync(employee.UserId, cancellationToken);

        return ToResponse(employee, user);
    }

    public async Task<Result<EmployeeResponse>> CreateAsync(CreateEmployeeRequest request, CancellationToken cancellationToken)
    {
        var role = await roleRepository.GetByNameAsync(request.Role, cancellationToken);
        if (role is null)
        {
            return Result<EmployeeResponse>.ValidationFailure("employee.invalid_role", "El rol indicado no existe.");
        }

        var existing = await userRepository.GetByUsernameOrEmailAsync(request.Username, cancellationToken);
        if (existing is not null)
        {
            return Result<EmployeeResponse>.Conflict("user.already_exists", "El nombre de usuario o correo ya está en uso.");
        }

        existing = await userRepository.GetByUsernameOrEmailAsync(request.Email, cancellationToken);
        if (existing is not null)
        {
            return Result<EmployeeResponse>.Conflict("user.already_exists", "El nombre de usuario o correo ya está en uso.");
        }

        try
        {
            var user = User.Create(request.Username, request.Email, passwordHasher.Hash(request.Password));
            user.AssignRole(role);
            await userRepository.AddAsync(user, cancellationToken);

            var employee = Employee.Create(user.Id, request.FirstName, request.LastName, request.Phone);
            await employeeRepository.AddAsync(employee, cancellationToken);
            await employeeRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(employee, user);
        }
        catch (DomainException exception)
        {
            return Result<EmployeeResponse>.ValidationFailure("employee.invalid", exception.Message);
        }
    }

    public async Task<Result<EmployeeResponse>> UpdateAsync(Guid id, UpdateEmployeeRequest request, CancellationToken cancellationToken)
    {
        var employee = await employeeRepository.GetByIdAsync(id, cancellationToken);
        if (employee is null)
        {
            return Result<EmployeeResponse>.NotFound("employee.not_found", "El empleado no existe.");
        }

        try
        {
            employee.Update(request.FirstName, request.LastName, request.Phone, request.IsActive);
            await employeeRepository.SaveChangesAsync(cancellationToken);

            var user = await userRepository.GetByIdAsync(employee.UserId, cancellationToken);

            return ToResponse(employee, user);
        }
        catch (DomainException exception)
        {
            return Result<EmployeeResponse>.ValidationFailure("employee.invalid", exception.Message);
        }
    }

    internal static EmployeeResponse ToResponse(Employee employee, User? user) => new(
        employee.Id,
        employee.UserId,
        user?.Username ?? string.Empty,
        user?.Email ?? string.Empty,
        employee.FirstName,
        employee.LastName,
        employee.Phone,
        employee.IsActive,
        user?.UserRoles.Select(ur => ur.Role?.Name ?? string.Empty).Where(static name => name.Length > 0).ToList() ?? []);
}

public sealed class RoleAdminService(
    IRoleRepository roleRepository,
    IUserRepository userRepository,
    IEmployeeRepository employeeRepository) : IRoleAdminService
{
    public async Task<Result<IReadOnlyCollection<RoleResponse>>> GetRolesAsync(CancellationToken cancellationToken)
    {
        var roles = await roleRepository.GetAllWithPermissionsAsync(cancellationToken);

        return roles.Select(ToRoleResponse).ToList();
    }

    public async Task<Result<IReadOnlyCollection<PermissionResponse>>> GetPermissionsAsync(CancellationToken cancellationToken)
    {
        var permissions = await roleRepository.GetPermissionsAsync(cancellationToken);

        return permissions.Select(p => new PermissionResponse(p.Name, p.Description)).ToList();
    }

    public async Task<Result<RoleResponse>> GrantPermissionAsync(GrantPermissionRequest request, CancellationToken cancellationToken)
    {
        var role = await roleRepository.GetByNameAsync(request.Role, cancellationToken);
        if (role is null)
        {
            return Result<RoleResponse>.NotFound("role.not_found", "El rol no existe.");
        }

        var permission = await roleRepository.GetPermissionByNameAsync(request.Permission, cancellationToken);
        if (permission is null)
        {
            return Result<RoleResponse>.NotFound("permission.not_found", "El permiso no existe.");
        }

        role.GrantPermission(permission);
        await roleRepository.SaveChangesAsync(cancellationToken);

        var refreshed = await roleRepository.GetByNameAsync(request.Role, cancellationToken);

        return ToRoleResponse(refreshed ?? role);
    }

    public async Task<Result<RoleResponse>> RevokePermissionAsync(GrantPermissionRequest request, CancellationToken cancellationToken)
    {
        var role = await roleRepository.GetByNameAsync(request.Role, cancellationToken);
        if (role is null)
        {
            return Result<RoleResponse>.NotFound("role.not_found", "El rol no existe.");
        }

        var permission = await roleRepository.GetPermissionByNameAsync(request.Permission, cancellationToken);
        if (permission is null)
        {
            return Result<RoleResponse>.NotFound("permission.not_found", "El permiso no existe.");
        }

        role.RevokePermission(permission);
        await roleRepository.SaveChangesAsync(cancellationToken);

        var refreshed = await roleRepository.GetByNameAsync(request.Role, cancellationToken);

        return ToRoleResponse(refreshed ?? role);
    }

    public async Task<Result<EmployeeResponse>> AssignRoleAsync(AssignRoleRequest request, CancellationToken cancellationToken)
    {
        var user = await userRepository.GetByIdAsync(request.UserId, cancellationToken);
        if (user is null)
        {
            return Result<EmployeeResponse>.NotFound("user.not_found", "El usuario no existe.");
        }

        var role = await roleRepository.GetByNameAsync(request.Role, cancellationToken);
        if (role is null)
        {
            return Result<EmployeeResponse>.NotFound("role.not_found", "El rol no existe.");
        }

        user.AssignRole(role);
        await userRepository.SaveChangesAsync(cancellationToken);

        var employee = await employeeRepository.GetByUserIdAsync(user.Id, cancellationToken);

        return employee is null
            ? Result<EmployeeResponse>.NotFound("employee.not_found", "El usuario no tiene ficha de empleado.")
            : EmployeeService.ToResponse(employee, user);
    }

    public async Task<Result<EmployeeResponse>> RemoveRoleAsync(AssignRoleRequest request, CancellationToken cancellationToken)
    {
        var user = await userRepository.GetByIdAsync(request.UserId, cancellationToken);
        if (user is null)
        {
            return Result<EmployeeResponse>.NotFound("user.not_found", "El usuario no existe.");
        }

        var role = await roleRepository.GetByNameAsync(request.Role, cancellationToken);
        if (role is null)
        {
            return Result<EmployeeResponse>.NotFound("role.not_found", "El rol no existe.");
        }

        user.RemoveRole(role);
        await userRepository.SaveChangesAsync(cancellationToken);

        var employee = await employeeRepository.GetByUserIdAsync(user.Id, cancellationToken);

        return employee is null
            ? Result<EmployeeResponse>.NotFound("employee.not_found", "El usuario no tiene ficha de empleado.")
            : EmployeeService.ToResponse(employee, user);
    }

    private static RoleResponse ToRoleResponse(Role role) => new(
        role.Id,
        role.Name,
        role.Description,
        role.RolePermissions
            .Select(rp => rp.Permission?.Name ?? string.Empty)
            .Where(static name => name.Length > 0)
            .ToList());
}