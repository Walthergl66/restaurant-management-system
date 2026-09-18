using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Finance;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class IncomeRepository(ApplicationDbContext dbContext) : IIncomeRepository
{
    public async Task<IReadOnlyCollection<Income>> GetAsync(DateTime fromUtc, DateTime toUtc, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Income>()
            .AsNoTracking()
            .Where(i => i.OccurredAtUtc >= fromUtc && i.OccurredAtUtc <= toUtc)
            .OrderByDescending(i => i.OccurredAtUtc)
            .ToListAsync(cancellationToken);
    }

    public async Task<decimal> GetTotalAsync(DateTime fromUtc, DateTime toUtc, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Income>()
            .Where(i => i.OccurredAtUtc >= fromUtc && i.OccurredAtUtc <= toUtc)
            .SumAsync(i => (decimal?)i.Amount, cancellationToken) ?? 0m;
    }

    public async Task AddAsync(Income income, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<Income>().AddAsync(income, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}

public sealed class ExpenseRepository(ApplicationDbContext dbContext) : IExpenseRepository
{
    public async Task<IReadOnlyCollection<Expense>> GetAsync(DateTime fromUtc, DateTime toUtc, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Expense>()
            .AsNoTracking()
            .Where(e => e.OccurredAtUtc >= fromUtc && e.OccurredAtUtc <= toUtc)
            .OrderByDescending(e => e.OccurredAtUtc)
            .ToListAsync(cancellationToken);
    }

    public async Task<decimal> GetTotalAsync(DateTime fromUtc, DateTime toUtc, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Expense>()
            .Where(e => e.OccurredAtUtc >= fromUtc && e.OccurredAtUtc <= toUtc)
            .SumAsync(e => (decimal?)e.Amount, cancellationToken) ?? 0m;
    }

    public async Task AddAsync(Expense expense, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<Expense>().AddAsync(expense, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}