using Restaurant.Domain.Cash;

namespace Restaurant.Application.Common.Abstractions;

public interface ICashRegisterRepository
{
    Task<IReadOnlyCollection<CashRegister>> GetAllAsync(CancellationToken cancellationToken = default);

    Task<CashRegister?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task AddAsync(CashRegister cashRegister, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}

public interface ICashOpeningRepository
{
    Task<CashOpening?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task<CashOpening?> GetWithMovementsAsync(Guid id, CancellationToken cancellationToken = default);

    Task<CashOpening?> GetOpenByRegisterAsync(Guid cashRegisterId, CancellationToken cancellationToken = default);

    Task<CashOpening?> GetCurrentOpenAsync(CancellationToken cancellationToken = default);

    Task AddAsync(CashOpening cashOpening, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}