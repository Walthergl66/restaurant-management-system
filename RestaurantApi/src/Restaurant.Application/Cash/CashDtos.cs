using Restaurant.Application.Common;

namespace Restaurant.Application.Cash;

public sealed record CashRegisterResponse(Guid Id, string Name, string? Description, bool IsActive);

public sealed record CreateCashRegisterRequest(string Name, string? Description);

public sealed record UpdateCashRegisterRequest(string Name, string? Description, bool IsActive);

public sealed record OpenCashRequest(Guid CashRegisterId, decimal InitialFund);

public sealed record CashMovementRequest(string Type, decimal Amount, string? Reference, string? Notes);

public sealed record CloseCashRequest(decimal CountedAmount);

public sealed record CashMovementResponse(
    Guid Id,
    string Type,
    decimal Amount,
    decimal SignedAmount,
    string? Reference,
    string? Notes,
    Guid CreatedByUserId,
    DateTime CreatedAtUtc);

public sealed record CashOpeningResponse(
    Guid Id,
    Guid CashRegisterId,
    Guid OpenedByUserId,
    DateTime OpenedAtUtc,
    decimal InitialFund,
    string Status,
    Guid? ClosedByUserId,
    DateTime? ClosedAtUtc,
    decimal ExpectedAmount,
    decimal? CountedAmount,
    decimal? Difference,
    decimal TotalIncome,
    decimal TotalExpense,
    IReadOnlyCollection<CashMovementResponse> Movements);

public interface ICashService
{
    Task<Result<IReadOnlyCollection<CashRegisterResponse>>> GetRegistersAsync(CancellationToken cancellationToken);

    Task<Result<CashRegisterResponse>> CreateRegisterAsync(CreateCashRegisterRequest request, CancellationToken cancellationToken);

    Task<Result<CashRegisterResponse>> UpdateRegisterAsync(Guid id, UpdateCashRegisterRequest request, CancellationToken cancellationToken);

    Task<Result<CashOpeningResponse>> OpenAsync(OpenCashRequest request, Guid userId, CancellationToken cancellationToken);

    Task<Result<CashOpeningResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<CashOpeningResponse>> GetCurrentAsync(CancellationToken cancellationToken);

    Task<Result<CashOpeningResponse>> RegisterMovementAsync(Guid id, CashMovementRequest request, Guid userId, CancellationToken cancellationToken);

    Task<Result<CashOpeningResponse>> CloseAsync(Guid id, CloseCashRequest request, Guid userId, CancellationToken cancellationToken);
}