using Restaurant.Application.Common;

namespace Restaurant.Application.Administration;

public sealed record EmployeeResponse(
    Guid Id,
    Guid UserId,
    string Username,
    string Email,
    string FirstName,
    string LastName,
    string? Phone,
    bool IsActive,
    IReadOnlyCollection<string> Roles);

public sealed record CreateEmployeeRequest(
    string Username,
    string Email,
    string Password,
    string FirstName,
    string LastName,
    string? Phone,
    string Role);

public sealed record UpdateEmployeeRequest(
    string FirstName,
    string LastName,
    string? Phone,
    bool IsActive);

public sealed record RoleResponse(
    Guid Id,
    string Name,
    string? Description,
    IReadOnlyCollection<string> Permissions);

public sealed record PermissionResponse(string Name, string? Description);

public sealed record GrantPermissionRequest(string Role, string Permission);

public sealed record AssignRoleRequest(Guid UserId, string Role);

public interface IEmployeeService
{
    Task<Result<IReadOnlyCollection<EmployeeResponse>>> GetAllAsync(CancellationToken cancellationToken);

    Task<Result<EmployeeResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<EmployeeResponse>> CreateAsync(CreateEmployeeRequest request, CancellationToken cancellationToken);

    Task<Result<EmployeeResponse>> UpdateAsync(Guid id, UpdateEmployeeRequest request, CancellationToken cancellationToken);
}

public interface IRoleAdminService
{
    Task<Result<IReadOnlyCollection<RoleResponse>>> GetRolesAsync(CancellationToken cancellationToken);

    Task<Result<IReadOnlyCollection<PermissionResponse>>> GetPermissionsAsync(CancellationToken cancellationToken);

    Task<Result<RoleResponse>> GrantPermissionAsync(GrantPermissionRequest request, CancellationToken cancellationToken);

    Task<Result<RoleResponse>> RevokePermissionAsync(GrantPermissionRequest request, CancellationToken cancellationToken);

    Task<Result<EmployeeResponse>> AssignRoleAsync(AssignRoleRequest request, CancellationToken cancellationToken);

    Task<Result<EmployeeResponse>> RemoveRoleAsync(AssignRoleRequest request, CancellationToken cancellationToken);
}