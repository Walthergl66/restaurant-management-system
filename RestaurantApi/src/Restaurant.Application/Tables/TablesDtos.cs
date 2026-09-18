using Restaurant.Application.Common;

namespace Restaurant.Application.Tables;

public sealed record TableResponse(
    Guid Id,
    string Name,
    int Capacity,
    string TableStatus,
    string? Location,
    Guid? CurrentAccountId);

public sealed record CreateTableRequest(string Name, int Capacity, string? Location);

public sealed record UpdateTableRequest(string Name, int Capacity, string? Location);

public sealed record AccountResponse(
    Guid Id,
    string AccountNumber,
    Guid? TableId,
    string Status,
    decimal Subtotal,
    decimal Discount,
    decimal Tip,
    decimal Total,
    decimal AmountDue,
    decimal PaidAmount,
    DateTime? OpenedAt,
    DateTime? ClosedAt);

public sealed record CreateAccountRequest(string AccountNumber, Guid? TableId);

public sealed record UpdateAccountTotalsRequest(decimal Subtotal, decimal Discount, decimal Tip);

public interface ITableService
{
    Task<Result<IReadOnlyCollection<TableResponse>>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken);

    Task<Result<TableResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<TableResponse>> CreateAsync(CreateTableRequest request, CancellationToken cancellationToken);

    Task<Result<TableResponse>> UpdateAsync(Guid id, UpdateTableRequest request, CancellationToken cancellationToken);

    Task<Result> SetStatusAsync(Guid id, int tableStatus, CancellationToken cancellationToken);
}

public interface ITableAccountService
{
    Task<Result<AccountResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<IReadOnlyCollection<AccountResponse>>> GetOpenAsync(CancellationToken cancellationToken);

    Task<Result<AccountResponse>> OpenAsync(CreateAccountRequest request, CancellationToken cancellationToken);

    Task<Result<AccountResponse>> UpdateTotalsAsync(Guid id, UpdateAccountTotalsRequest request, CancellationToken cancellationToken);

    Task<Result> CloseAccountAsync(Guid id, CancellationToken cancellationToken);
}