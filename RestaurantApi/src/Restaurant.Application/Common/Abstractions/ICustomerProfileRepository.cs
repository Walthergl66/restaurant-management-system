using Restaurant.Domain.Customers;

namespace Restaurant.Application.Common.Abstractions;

public interface ICustomerProfileRepository
{
    Task<CustomerProfile?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task<CustomerProfile?> GetWithAddressesAsync(Guid id, CancellationToken cancellationToken = default);

    Task<CustomerProfile?> GetByUserAsync(Guid userId, CancellationToken cancellationToken = default);

    Task AddAsync(CustomerProfile customerProfile, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}