using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;

namespace Restaurant.Application.Auth;

public sealed record LoginCommand(string UsernameOrEmail, string Password);

public sealed record RefreshCommand(string RefreshToken);

public sealed record LogoutCommand(string RefreshToken);

public interface IAuthService
{
    Task<Result<AuthResponse>> LoginAsync(LoginCommand command, CancellationToken cancellationToken);

    Task<Result<AuthResponse>> RefreshAsync(RefreshCommand command, CancellationToken cancellationToken);

    Task<Result> LogoutAsync(LogoutCommand command, CancellationToken cancellationToken);
}