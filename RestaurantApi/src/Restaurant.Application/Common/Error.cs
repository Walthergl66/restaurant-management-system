namespace Restaurant.Application.Common;

public enum ErrorType
{
    Validation,
    BusinessRule,
    NotFound,
    Unauthorized,
    Forbidden,
    Conflict,
    Unexpected,
}

public sealed record Error(string Code, string Message, ErrorType Type);