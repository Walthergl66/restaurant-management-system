using Restaurant.Domain.Catalog;

namespace Restaurant.Application.Common.Abstractions;

public interface IPreparationAreaRepository
{
    Task<IReadOnlyCollection<PreparationArea>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken = default);

    Task<PreparationArea?> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task AddAsync(PreparationArea preparationArea, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}