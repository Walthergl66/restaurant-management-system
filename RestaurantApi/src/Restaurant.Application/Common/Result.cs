namespace Restaurant.Application.Common;

public class Result
{
    protected Result(bool isSuccess, Error? error)
    {
        IsSuccess = isSuccess;
        Error = error;
    }

    public bool IsSuccess { get; }

    public bool IsFailure => !IsSuccess;

    public Error? Error { get; }

    public static Result Success() => new(true, null);

    public static Result Failure(Error error) => new(false, error);

    public static Result ValidationFailure(string code, string message) =>
        new(false, new Error(code, message, ErrorType.Validation));

    public static Result BusinessRuleFailure(string code, string message) =>
        new(false, new Error(code, message, ErrorType.BusinessRule));

    public static Result NotFound(string code, string message = "Recurso no encontrado.") =>
        new(false, new Error(code, message, ErrorType.NotFound));

    public static Result Conflict(string code, string message) =>
        new(false, new Error(code, message, ErrorType.Conflict));
}

public sealed class Result<TValue> : Result
{
    private Result(bool isSuccess, Error? error, TValue? value)
        : base(isSuccess, error)
    {
        Value = value;
    }

    public TValue? Value { get; }

    public static Result<TValue> Success(TValue value) => new(true, null, value);

    public new static Result<TValue> Failure(Error error) => new(false, error, default);

    public new static Result<TValue> ValidationFailure(string code, string message) =>
        new(false, new Error(code, message, ErrorType.Validation), default);

    public new static Result<TValue> BusinessRuleFailure(string code, string message) =>
        new(false, new Error(code, message, ErrorType.BusinessRule), default);

    public new static Result<TValue> NotFound(string code, string message = "Recurso no encontrado.") =>
        new(false, new Error(code, message, ErrorType.NotFound), default);

    public new static Result<TValue> Conflict(string code, string message) =>
        new(false, new Error(code, message, ErrorType.Conflict), default);

    public static implicit operator Result<TValue>(TValue value) => Success(value);
}