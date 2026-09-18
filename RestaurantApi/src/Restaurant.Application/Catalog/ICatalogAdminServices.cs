using Restaurant.Application.Common;

namespace Restaurant.Application.Catalog;

public interface IPreparationAreaService
{
    Task<Result<IReadOnlyCollection<PreparationAreaResponse>>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken);

    Task<Result<PreparationAreaResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<PreparationAreaResponse>> CreateAsync(CreatePreparationAreaRequest request, CancellationToken cancellationToken);

    Task<Result<PreparationAreaResponse>> UpdateAsync(Guid id, UpdatePreparationAreaRequest request, CancellationToken cancellationToken);
}

public interface IExtraService
{
    Task<Result<IReadOnlyCollection<ExtraResponse>>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken);

    Task<Result<ExtraResponse>> CreateAsync(CreateExtraRequest request, CancellationToken cancellationToken);

    Task<Result<ExtraResponse>> UpdateAsync(Guid id, UpdateExtraRequest request, CancellationToken cancellationToken);
}