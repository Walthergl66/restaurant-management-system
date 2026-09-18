using Restaurant.Domain.Finance;

namespace Restaurant.Application.Common.Abstractions;

public interface IIncomeRepository
{
    Task<IReadOnlyCollection<Income>> GetAsync(DateTime fromUtc, DateTime toUtc, CancellationToken cancellationToken = default);

    Task<decimal> GetTotalAsync(DateTime fromUtc, DateTime toUtc, CancellationToken cancellationToken = default);

    Task AddAsync(Income income, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}

public interface IExpenseRepository
{
    Task<IReadOnlyCollection<Expense>> GetAsync(DateTime fromUtc, DateTime toUtc, CancellationToken cancellationToken = default);

    Task<decimal> GetTotalAsync(DateTime fromUtc, DateTime toUtc, CancellationToken cancellationToken = default);

    Task AddAsync(Expense expense, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}