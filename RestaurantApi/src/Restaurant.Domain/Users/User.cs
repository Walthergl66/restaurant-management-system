using Restaurant.Domain.Common;

namespace Restaurant.Domain.Users;

public sealed class User : AuditableEntity
{
    private readonly List<UserRole> _userRoles = [];
    private readonly List<RefreshToken> _refreshTokens = [];

    public string Username { get; private set; } = string.Empty;

    public string Email { get; private set; } = string.Empty;

    public string PasswordHash { get; private set; } = string.Empty;

    public bool IsActive { get; private set; } = true;

    public DateTime LastLoginAtUtc { get; private set; }

    public IReadOnlyCollection<UserRole> UserRoles => _userRoles.AsReadOnly();

    public IReadOnlyCollection<RefreshToken> RefreshTokens => _refreshTokens.AsReadOnly();

    public static User Create(string username, string email, string passwordHash, bool isActive = true)
    {
        if (string.IsNullOrWhiteSpace(username))
        {
            throw new DomainException("El nombre de usuario es obligatorio.");
        }

        if (string.IsNullOrWhiteSpace(email))
        {
            throw new DomainException("El correo electrónico es obligatorio.");
        }

        if (string.IsNullOrWhiteSpace(passwordHash))
        {
            throw new DomainException("El hash de la contraseña es obligatorio.");
        }

        return new User
        {
            Username = username.Trim().ToLowerInvariant(),
            Email = email.Trim().ToLowerInvariant(),
            PasswordHash = passwordHash,
            IsActive = isActive,
        };
    }

    public void AssignRole(Role role)
    {
        if (_userRoles.Any(ur => ur.RoleId == role.Id))
        {
            return;
        }

        _userRoles.Add(new UserRole { UserId = Id, RoleId = role.Id });
    }

    public void RemoveRole(Role role)
    {
        var userRole = _userRoles.FirstOrDefault(ur => ur.RoleId == role.Id);
        if (userRole is not null)
        {
            _userRoles.Remove(userRole);
        }
    }

    public void Deactivate()
    {
        IsActive = false;
    }

    public void RegisterLogin()
    {
        LastLoginAtUtc = DateTime.UtcNow;
    }

    public void AddRefreshToken(string token, DateTime expiresAtUtc)
    {
        _refreshTokens.RemoveAll(rt => rt.IsRevoked || rt.ExpiresAtUtc < DateTime.UtcNow);
        _refreshTokens.Add(RefreshToken.Create(Id, token, expiresAtUtc));
    }
}

public sealed class UserRole
{
    public Guid UserId { get; set; }

    public Guid RoleId { get; set; }

    public User? User { get; set; }

    public Role? Role { get; set; }
}

public sealed class RefreshToken : BaseEntity
{
    public Guid UserId { get; private set; }

    public string Token { get; private set; } = string.Empty;

    public DateTime ExpiresAtUtc { get; private set; }

    public bool IsRevoked { get; private set; }

    public DateTime? RevokedAtUtc { get; private set; }

    public User? User { get; set; }

    public static RefreshToken Create(Guid userId, string token, DateTime expiresAtUtc)
    {
        if (string.IsNullOrWhiteSpace(token))
        {
            throw new DomainException("El token de refresco es obligatorio.");
        }

        if (expiresAtUtc <= DateTime.UtcNow)
        {
            throw new DomainException("La fecha de expiración del token debe ser futura.");
        }

        return new RefreshToken
        {
            UserId = userId,
            Token = token,
            ExpiresAtUtc = expiresAtUtc,
        };
    }

    public void Revoke()
    {
        if (IsRevoked)
        {
            return;
        }

        IsRevoked = true;
        RevokedAtUtc = DateTime.UtcNow;
    }
}