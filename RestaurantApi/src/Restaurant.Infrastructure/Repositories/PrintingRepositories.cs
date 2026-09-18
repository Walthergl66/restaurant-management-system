using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Printing;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class PrinterRepository(ApplicationDbContext dbContext) : IPrinterRepository
{
    public async Task<Printer?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Printer>().FindAsync([id], cancellationToken);
    }

    public async Task<IReadOnlyCollection<Printer>> GetActiveAsync(CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Printer>()
            .AsNoTracking()
            .Where(p => p.IsActive)
            .OrderBy(p => p.Name)
            .ToListAsync(cancellationToken);
    }

    public async Task<Printer?> GetForAreaAsync(Guid preparationAreaId, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<Printer>()
            .AsNoTracking()
            .Where(p => p.IsActive && p.PreparationAreaIds.Contains(preparationAreaId))
            .FirstOrDefaultAsync(cancellationToken);
    }

    public async Task AddAsync(Printer printer, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<Printer>().AddAsync(printer, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}

public sealed class PrintJobRepository(ApplicationDbContext dbContext) : IPrintJobRepository
{
    public async Task<PrintJob?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<PrintJob>().FindAsync([id], cancellationToken);
    }

    public async Task<PrintJob?> GetByIdempotencyKeyAsync(string idempotencyKey, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<PrintJob>()
            .FirstOrDefaultAsync(j => j.IdempotencyKey == idempotencyKey, cancellationToken);
    }

    public async Task AddAsync(PrintJob printJob, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<PrintJob>().AddAsync(printJob, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}