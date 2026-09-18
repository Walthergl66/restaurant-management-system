using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Billing;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class InvoiceRepository(ApplicationDbContext dbContext) : IInvoiceRepository
{
    public async Task<Invoice?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Invoice>().FindAsync([id], cancellationToken);
    }

    public async Task<Invoice?> GetWithItemsAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Invoice>()
            .Include(i => i.Items)
            .FirstOrDefaultAsync(i => i.Id == id, cancellationToken);
    }

    public async Task<Invoice?> GetBySaleAsync(Guid saleId, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Invoice>()
            .Include(i => i.Items)
            .FirstOrDefaultAsync(i => i.SaleId == saleId, cancellationToken);
    }

    public async Task AddAsync(Invoice invoice, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<Invoice>().AddAsync(invoice, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}