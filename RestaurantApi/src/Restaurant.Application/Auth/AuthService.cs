using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;

namespace Restaurant.Application.Auth;

public sealed class AuthService(
    IUserRepository userRepository,
    IPasswordHasher passwordHasher,
    IJwtTokenGenerator jwtTokenGenerator) : IAuthService
{
    public async Task<Result<AuthResponse>> LoginAsync(LoginCommand command, CancellationToken cancellationToken)
    {
        var user = await userRepository.GetByUsernameOrEmailAsync(command.UsernameOrEmail, cancellationToken);
        if (user is null || !passwordHasher.Verify(command.Password, user.PasswordHash))
        {
            return Result<AuthResponse>.ValidationFailure("auth.invalid_credentials", "Credenciales inválidas.");
        }

        if (!user.IsActive)
        {
            return Result<AuthResponse>.BusinessRuleFailure("auth.user_inactive", "El usuario está inactivo.");
        }

        user.RegisterLogin();

        var accessToken = jwtTokenGenerator.GenerateAccessToken(user);
        var refreshToken = jwtTokenGenerator.GenerateRefreshToken();

        user.AddRefreshToken(refreshToken.Token, refreshToken.ExpiresAtUtc);

        await userRepository.SaveChangesAsync(cancellationToken);

        return Result<AuthResponse>.Success(BuildAuthResponse(user, accessToken, refreshToken));
    }

    public async Task<Result<AuthResponse>> RefreshAsync(RefreshCommand command, CancellationToken cancellationToken)
    {
        var refreshToken = await userRepository.GetRefreshTokenAsync(command.RefreshToken, cancellationToken);
        if (refreshToken?.User is null)
        {
            return Result<AuthResponse>.ValidationFailure("auth.invalid_refresh_token", "Token de refresco inválido.");
        }

        if (refreshToken.IsRevoked)
        {
            return Result<AuthResponse>.BusinessRuleFailure("auth.refresh_token_revoked", "El token de refresco fue revocado.");
        }

        if (refreshToken.ExpiresAtUtc < DateTime.UtcNow)
        {
            return Result<AuthResponse>.BusinessRuleFailure("auth.refresh_token_expired", "El token de refresco expiró.");
        }

        if (!refreshToken.User.IsActive)
        {
            return Result<AuthResponse>.BusinessRuleFailure("auth.user_inactive", "El usuario está inactivo.");
        }

        refreshToken.Revoke();

        var accessToken = jwtTokenGenerator.GenerateAccessToken(refreshToken.User);
        var newRefreshToken = jwtTokenGenerator.GenerateRefreshToken();

        refreshToken.User.AddRefreshToken(newRefreshToken.Token, newRefreshToken.ExpiresAtUtc);

        await userRepository.SaveChangesAsync(cancellationToken);

        return Result<AuthResponse>.Success(BuildAuthResponse(refreshToken.User, accessToken, newRefreshToken));
    }

    public async Task<Result> LogoutAsync(LogoutCommand command, CancellationToken cancellationToken)
    {
        var refreshToken = await userRepository.GetRefreshTokenAsync(command.RefreshToken, cancellationToken);
        if (refreshToken is not null)
        {
            refreshToken.Revoke();
            await userRepository.SaveChangesAsync(cancellationToken);
        }

        return Result.Success();
    }

    private static AuthResponse BuildAuthResponse(
        Domain.Users.User user,
        GeneratedToken accessToken,
        GeneratedToken refreshToken)
    {
        return new AuthResponse(
            accessToken.Token,
            refreshToken.Token,
            accessToken.ExpiresAtUtc,
            user.Id,
            user.Username,
            user.Email,
            user.UserRoles.Select(ur => ur.Role?.Name).Where(static name => name is not null).Cast<string>().ToList(),
            user.UserRoles
                .SelectMany(ur => ur.Role?.RolePermissions ?? [])
                .Select(rp => rp.Permission?.Name)
                .Where(static name => name is not null)
                .Cast<string>()
                .ToList());
    }
}