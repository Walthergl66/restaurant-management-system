using System.Security.Cryptography;
using Microsoft.AspNetCore.Cryptography.KeyDerivation;
using Restaurant.Application.Common.Abstractions;

namespace Restaurant.Infrastructure.Authentication;

public sealed class PasswordHasher : IPasswordHasher
{
    private const int Iterations = 100_000;
    private const int SaltSize = 16;
    private const int KeySize = 32;

    public string Hash(string password)
    {
        var salt = RandomNumberGenerator.GetBytes(SaltSize);
        var key = KeyDerivation.Pbkdf2(password, salt, KeyDerivationPrf.HMACSHA256, Iterations, KeySize);

        var result = new byte[SaltSize + KeySize];
        Buffer.BlockCopy(salt, 0, result, 0, SaltSize);
        Buffer.BlockCopy(key, 0, result, SaltSize, KeySize);

        return Convert.ToBase64String(result);
    }

    public bool Verify(string password, string passwordHash)
    {
        var bytes = Convert.FromBase64String(passwordHash);
        if (bytes.Length != SaltSize + KeySize)
        {
            return false;
        }

        var salt = new byte[SaltSize];
        byte[] expected = new byte[KeySize];
        Buffer.BlockCopy(bytes, 0, salt, 0, SaltSize);
        Buffer.BlockCopy(bytes, SaltSize, expected, 0, KeySize);

        var actual = KeyDerivation.Pbkdf2(password, salt, KeyDerivationPrf.HMACSHA256, Iterations, KeySize);

        return CryptographicOperations.FixedTimeEquals(expected, actual);
    }
}