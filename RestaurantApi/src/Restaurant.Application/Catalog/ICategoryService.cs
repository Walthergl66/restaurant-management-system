using Restaurant.Application.Common;

namespace Restaurant.Application.Catalog;

public interface ICategoryService
{
    Task<Result<IReadOnlyCollection<CategoryResponse>>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken);

    Task<Result<CategoryResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<CategoryResponse>> CreateAsync(CreateCategoryRequest request, CancellationToken cancellationToken);

    Task<Result<CategoryResponse>> UpdateAsync(Guid id, UpdateCategoryRequest request, CancellationToken cancellationToken);

    Task<Result> ActivateAsync(Guid id, CancellationToken cancellationToken);

    Task<Result> DeactivateAsync(Guid id, CancellationToken cancellationToken);
}