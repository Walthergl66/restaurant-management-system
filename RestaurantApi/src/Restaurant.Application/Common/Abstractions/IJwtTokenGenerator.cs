using Restaurant.Domain.Users;

namespace Restaurant.Application.Common.Abstractions;

public sealed record GeneratedToken(string Token, DateTime ExpiresAtUtc);

public interface IJwtTokenGenerator
{
    GeneratedToken GenerateAccessToken(User user);

    GeneratedToken GenerateRefreshToken();
}