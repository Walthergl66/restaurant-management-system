using Restaurant.Domain.Tables;

namespace Restaurant.Application.Common.Abstractions;

public interface IRestaurantTableRepository
{
    Task<IReadOnlyCollection<RestaurantTable>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken = default);

    Task<RestaurantTable?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task AddAsync(RestaurantTable table, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}

public interface ITableAccountRepository
{
    Task<TableAccount?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task<TableAccount?> GetByNumberAsync(string accountNumber, CancellationToken cancellationToken = default);

    Task<IReadOnlyCollection<TableAccount>> GetOpenAccountsAsync(CancellationToken cancellationToken = default);

    Task AddAsync(TableAccount account, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}