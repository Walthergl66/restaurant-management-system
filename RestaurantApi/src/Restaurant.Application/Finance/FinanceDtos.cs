using Restaurant.Application.Common;

namespace Restaurant.Application.Finance;

public sealed record IncomeResponse(
    Guid Id,
    decimal Amount,
    string Source,
    Guid? ReferenceId,
    string? Description,
    DateTime OccurredAtUtc,
    Guid? CreatedByUserId);

public sealed record ExpenseResponse(
    Guid Id,
    decimal Amount,
    string Category,
    string Description,
    string? Reference,
    DateTime OccurredAtUtc,
    Guid? CreatedByUserId);

public sealed record CreateExpenseRequest(
    decimal Amount,
    string Category,
    string Description,
    string? Reference);

public sealed record FinancialResultResponse(
    DateTime FromUtc,
    DateTime ToUtc,
    decimal TotalIncome,
    decimal TotalExpenses,
    decimal Result);

public interface IFinanceService
{
    Task<Result<IReadOnlyCollection<IncomeResponse>>> GetIncomeAsync(DateTime? fromUtc, DateTime? toUtc, CancellationToken cancellationToken);

    Task<Result<IReadOnlyCollection<ExpenseResponse>>> GetExpensesAsync(DateTime? fromUtc, DateTime? toUtc, CancellationToken cancellationToken);

    Task<Result<ExpenseResponse>> CreateExpenseAsync(CreateExpenseRequest request, Guid userId, CancellationToken cancellationToken);

    Task<Result<FinancialResultResponse>> GetResultAsync(DateTime? fromUtc, DateTime? toUtc, CancellationToken cancellationToken);

    Task<Result<IncomeResponse>> RecordIncomeAsync(
        decimal amount,
        string source,
        Guid? referenceId,
        string? description,
        Guid? userId,
        CancellationToken cancellationToken);
}