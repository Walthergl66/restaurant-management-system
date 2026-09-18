using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Users;

namespace Restaurant.Infrastructure.Authentication;

public sealed class JwtOptions
{
    public const string SectionName = "Jwt";

    public string Secret { get; set; } = string.Empty;

    public string Issuer { get; set; } = string.Empty;

    public string Audience { get; set; } = string.Empty;

    public int AccessTokenMinutes { get; set; } = 15;

    public int RefreshTokenDays { get; set; } = 7;
}

public sealed class JwtTokenGenerator(IOptions<JwtOptions> options) : IJwtTokenGenerator
{
    private readonly JwtOptions _options = options.Value;

    public GeneratedToken GenerateAccessToken(User user)
    {
        var claims = new List<Claim>
        {
            new(JwtRegisteredClaimNames.Sub, user.Id.ToString()),
            new(JwtRegisteredClaimNames.Email, user.Email),
            new(JwtRegisteredClaimNames.UniqueName, user.Username),
        };

        foreach (var role in user.UserRoles.Select(ur => ur.Role?.Name).Where(static name => name is not null))
        {
            claims.Add(new Claim(ClaimTypes.Role, role!));
        }

        foreach (var permission in user.UserRoles
            .SelectMany(ur => ur.Role?.RolePermissions ?? [])
            .Select(rp => rp.Permission?.Name)
            .Where(static name => name is not null))
        {
            claims.Add(new Claim("permissions", permission!));
        }

        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_options.Secret));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);
        var expiresAtUtc = DateTime.UtcNow.AddMinutes(_options.AccessTokenMinutes);

        var token = new JwtSecurityToken(
            _options.Issuer,
            _options.Audience,
            claims,
            expires: expiresAtUtc,
            signingCredentials: credentials);

        var tokenValue = new JwtSecurityTokenHandler().WriteToken(token);
        return new GeneratedToken(tokenValue, expiresAtUtc);
    }

    public GeneratedToken GenerateRefreshToken()
    {
        var bytes = RandomNumberGenerator.GetBytes(64);
        return new GeneratedToken(Convert.ToBase64String(bytes), DateTime.UtcNow.AddDays(_options.RefreshTokenDays));
    }
}