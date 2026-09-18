using Restaurant.Domain.Common;

namespace Restaurant.Domain.Users;

public enum RoleName
{
    CLIENT = 1,
    WAITER = 2,
    SUPERVISOR = 3,
    ADMIN = 4,
}

public sealed class Role : BaseEntity
{
    private readonly List<RolePermission> _rolePermissions = [];
    private readonly List<UserRole> _userRoles = [];

    public string Name { get; private set; } = string.Empty;

    public string? Description { get; private set; }

    public IReadOnlyCollection<RolePermission> RolePermissions => _rolePermissions.AsReadOnly();

    public IReadOnlyCollection<UserRole> UserRoles => _userRoles.AsReadOnly();

    public static Role Create(RoleName roleName, string? description = null)
    {
        return new Role
        {
            Name = roleName.ToString(),
            Description = description,
        };
    }

    public void GrantPermission(Permission permission)
    {
        if (_rolePermissions.Any(rp => rp.PermissionId == permission.Id))
        {
            return;
        }

        _rolePermissions.Add(new RolePermission { RoleId = Id, PermissionId = permission.Id });
    }

    public void RevokePermission(Permission permission)
    {
        var pair = _rolePermissions.FirstOrDefault(rp => rp.PermissionId == permission.Id);
        if (pair is not null)
        {
            _rolePermissions.Remove(pair);
        }
    }
}

public sealed class Permission : BaseEntity
{
    private readonly List<RolePermission> _rolePermissions = [];

    public string Name { get; private set; } = string.Empty;

    public string? Description { get; private set; }

    public IReadOnlyCollection<RolePermission> RolePermissions => _rolePermissions.AsReadOnly();

    public static Permission Create(string name, string? description = null)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new DomainException("El nombre del permiso es obligatorio.");
        }

        return new Permission
        {
            Name = name.Trim(),
            Description = description,
        };
    }
}

public sealed class RolePermission
{
    public Guid RoleId { get; set; }

    public Guid PermissionId { get; set; }

    public Role? Role { get; set; }

    public Permission? Permission { get; set; }
}