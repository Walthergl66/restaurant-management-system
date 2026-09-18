using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Cash;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class CashRegisterRepository(ApplicationDbContext dbContext) : ICashRegisterRepository
{
    public async Task<IReadOnlyCollection<CashRegister>> GetAllAsync(CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<CashRegister>()
            .AsNoTracking()
            .OrderBy(r => r.Name)
            .ToListAsync(cancellationToken);
    }

    public async Task<CashRegister?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<CashRegister>().FindAsync([id], cancellationToken);
    }

    public async Task AddAsync(CashRegister cashRegister, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<CashRegister>().AddAsync(cashRegister, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}

public sealed class CashOpeningRepository(ApplicationDbContext dbContext) : ICashOpeningRepository
{
    public async Task<CashOpening?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<CashOpening>().FindAsync([id], cancellationToken);
    }

    public async Task<CashOpening?> GetWithMovementsAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<CashOpening>()
            .Include(o => o.Movements)
            .FirstOrDefaultAsync(o => o.Id == id, cancellationToken);
    }

    public async Task<CashOpening?> GetOpenByRegisterAsync(Guid cashRegisterId, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<CashOpening>()
            .Where(o => o.CashRegisterId == cashRegisterId && o.Status == CashRegisterStatus.OPEN)
            .FirstOrDefaultAsync(cancellationToken);
    }

    public async Task<CashOpening?> GetCurrentOpenAsync(CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<CashOpening>()
            .Include(o => o.Movements)
            .Where(o => o.Status == CashRegisterStatus.OPEN)
            .OrderByDescending(o => o.OpenedAtUtc)
            .FirstOrDefaultAsync(cancellationToken);
    }

    public async Task AddAsync(CashOpening cashOpening, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<CashOpening>().AddAsync(cashOpening, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}