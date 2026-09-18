using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Common;
using Restaurant.Domain.Catalog;

namespace Restaurant.Application.Catalog;

public sealed class CategoryService(ICategoryRepository categoryRepository) : ICategoryService
{
    public async Task<Result<IReadOnlyCollection<CategoryResponse>>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken)
    {
        var categories = await categoryRepository.GetAllAsync(onlyActive, cancellationToken);

        return categories
            .Select(ToResponse)
            .ToList();
    }

    public async Task<Result<CategoryResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var category = await categoryRepository.GetByIdAsync(id, cancellationToken);

        return category is null
            ? Result<CategoryResponse>.NotFound("category.not_found", "La categoría no existe.")
            : ToResponse(category);
    }

    public async Task<Result<CategoryResponse>> CreateAsync(CreateCategoryRequest request, CancellationToken cancellationToken)
    {
        try
        {
            var category = Category.Create(request.Name, request.Description, request.SortOrder);
            await categoryRepository.AddAsync(category, cancellationToken);
            await categoryRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(category);
        }
        catch (DomainException exception)
        {
            return Result<CategoryResponse>.ValidationFailure("category.invalid", exception.Message);
        }
    }

    public async Task<Result<CategoryResponse>> UpdateAsync(Guid id, UpdateCategoryRequest request, CancellationToken cancellationToken)
    {
        var category = await categoryRepository.GetByIdAsync(id, cancellationToken);
        if (category is null)
        {
            return Result<CategoryResponse>.NotFound("category.not_found", "La categoría no existe.");
        }

        try
        {
            category.Update(request.Name, request.Description, request.SortOrder);
            if (request.IsActive)
            {
                category.Activate();
            }
            else
            {
                category.Deactivate();
            }

            await categoryRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(category);
        }
        catch (DomainException exception)
        {
            return Result<CategoryResponse>.ValidationFailure("category.invalid", exception.Message);
        }
    }

    public async Task<Result> ActivateAsync(Guid id, CancellationToken cancellationToken)
    {
        var category = await categoryRepository.GetByIdAsync(id, cancellationToken);
        if (category is null)
        {
            return Result.NotFound("category.not_found", "La categoría no existe.");
        }

        category.Activate();
        await categoryRepository.SaveChangesAsync(cancellationToken);

        return Result.Success();
    }

    public async Task<Result> DeactivateAsync(Guid id, CancellationToken cancellationToken)
    {
        var category = await categoryRepository.GetByIdAsync(id, cancellationToken);
        if (category is null)
        {
            return Result.NotFound("category.not_found", "La categoría no existe.");
        }

        category.Deactivate();
        await categoryRepository.SaveChangesAsync(cancellationToken);

        return Result.Success();
    }

    private static CategoryResponse ToResponse(Category category) => new(
        category.Id,
        category.Name,
        category.Description,
        category.IsActive,
        category.SortOrder);
}