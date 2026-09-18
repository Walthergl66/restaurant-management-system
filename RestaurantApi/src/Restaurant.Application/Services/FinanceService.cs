using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Application.Finance;
using Restaurant.Domain.Common;
using Restaurant.Domain.Finance;

namespace Restaurant.Application.Services;

public sealed class FinanceService(
    IIncomeRepository incomeRepository,
    IExpenseRepository expenseRepository) : IFinanceService
{
    public async Task<Result<IReadOnlyCollection<IncomeResponse>>> GetIncomeAsync(DateTime? fromUtc, DateTime? toUtc, CancellationToken cancellationToken)
    {
        var (from, to) = NormalizeRange(fromUtc, toUtc);
        var income = await incomeRepository.GetAsync(from, to, cancellationToken);

        return income.Select(ToIncomeResponse).ToList();
    }

    public async Task<Result<IReadOnlyCollection<ExpenseResponse>>> GetExpensesAsync(DateTime? fromUtc, DateTime? toUtc, CancellationToken cancellationToken)
    {
        var (from, to) = NormalizeRange(fromUtc, toUtc);
        var expenses = await expenseRepository.GetAsync(from, to, cancellationToken);

        return expenses.Select(ToExpenseResponse).ToList();
    }

    public async Task<Result<ExpenseResponse>> CreateExpenseAsync(CreateExpenseRequest request, Guid userId, CancellationToken cancellationToken)
    {
        if (!Enum.TryParse<ExpenseCategory>(request.Category, true, out var category))
        {
            return Result<ExpenseResponse>.ValidationFailure("expense.invalid_category", "La categoría del egreso no es válida.");
        }

        try
        {
            var expense = Expense.Create(request.Amount, category, request.Description, request.Reference, userId);
            await expenseRepository.AddAsync(expense, cancellationToken);
            await expenseRepository.SaveChangesAsync(cancellationToken);

            return ToExpenseResponse(expense);
        }
        catch (DomainException exception)
        {
            return Result<ExpenseResponse>.ValidationFailure("expense.invalid", exception.Message);
        }
    }

    public async Task<Result<FinancialResultResponse>> GetResultAsync(DateTime? fromUtc, DateTime? toUtc, CancellationToken cancellationToken)
    {
        var (from, to) = NormalizeRange(fromUtc, toUtc);

        var totalIncome = await incomeRepository.GetTotalAsync(from, to, cancellationToken);
        var totalExpenses = await expenseRepository.GetTotalAsync(from, to, cancellationToken);

        return new FinancialResultResponse(from, to, totalIncome, totalExpenses, totalIncome - totalExpenses);
    }

    public async Task<Result<IncomeResponse>> RecordIncomeAsync(
        decimal amount,
        string source,
        Guid? referenceId,
        string? description,
        Guid? userId,
        CancellationToken cancellationToken)
    {
        if (!Enum.TryParse<IncomeSource>(source, true, out var incomeSource))
        {
            return Result<IncomeResponse>.ValidationFailure("income.invalid_source", "La fuente del ingreso no es válida.");
        }

        try
        {
            var income = Income.Create(amount, incomeSource, referenceId, description, userId);
            await incomeRepository.AddAsync(income, cancellationToken);
            await incomeRepository.SaveChangesAsync(cancellationToken);

            return ToIncomeResponse(income);
        }
        catch (DomainException exception)
        {
            return Result<IncomeResponse>.ValidationFailure("income.invalid", exception.Message);
        }
    }

    private static (DateTime From, DateTime To) NormalizeRange(DateTime? fromUtc, DateTime? toUtc)
    {
        var from = fromUtc ?? DateTime.UtcNow.Date.AddMonths(-1);
        var to = toUtc ?? DateTime.UtcNow;

        return (from, to);
    }

    private static IncomeResponse ToIncomeResponse(Income income) => new(
        income.Id,
        income.Amount,
        income.Source.ToString(),
        income.ReferenceId,
        income.Description,
        income.OccurredAtUtc,
        income.CreatedByUserId);

    private static ExpenseResponse ToExpenseResponse(Expense expense) => new(
        expense.Id,
        expense.Amount,
        expense.Category.ToString(),
        expense.Description,
        expense.Reference,
        expense.OccurredAtUtc,
        expense.CreatedByUserId);
}