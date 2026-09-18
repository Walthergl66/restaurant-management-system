using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Customers;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class CustomerProfileRepository(ApplicationDbContext dbContext) : ICustomerProfileRepository
{
    public async Task<CustomerProfile?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<CustomerProfile>().FindAsync([id], cancellationToken);
    }

    public async Task<CustomerProfile?> GetWithAddressesAsync(Guid id, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<CustomerProfile>()
            .Include(c => c.Addresses)
            .FirstOrDefaultAsync(c => c.Id == id, cancellationToken);
    }

    public async Task<CustomerProfile?> GetByUserAsync(Guid userId, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<CustomerProfile>()
            .Include(c => c.Addresses)
            .FirstOrDefaultAsync(c => c.UserId == userId, cancellationToken);
    }

    public async Task AddAsync(CustomerProfile customerProfile, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<CustomerProfile>().AddAsync(customerProfile, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}