using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Payments;
using Restaurant.Domain.Sales;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class PaymentRepository(ApplicationDbContext dbContext) : IPaymentRepository
{
    public async Task<Payment?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Payment>().FindAsync([id], cancellationToken);
    }

    public async Task AddAsync(Payment payment, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<Payment>().AddAsync(payment, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}

public sealed class SaleRepository(ApplicationDbContext dbContext) : ISaleRepository
{
    public async Task<Sale?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Sale>().FindAsync([id], cancellationToken);
    }

    public async Task<Sale?> GetWithItemsAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Sale>()
            .Include(s => s.Items)
            .FirstOrDefaultAsync(s => s.Id == id, cancellationToken);
    }

    public async Task<Sale?> GetByAccountAsync(Guid accountId, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Sale>()
            .Include(s => s.Items)
            .OrderByDescending(s => s.CreatedAtUtc)
            .FirstOrDefaultAsync(s => s.TableAccountId == accountId, cancellationToken);
    }

    public async Task AddAsync(Sale sale, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<Sale>().AddAsync(sale, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}