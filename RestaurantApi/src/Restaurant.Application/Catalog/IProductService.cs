using Restaurant.Application.Common;

namespace Restaurant.Application.Catalog;

public interface IProductService
{
    Task<Result<IReadOnlyCollection<ProductResponse>>> GetAllAsync(bool onlyAvailable, CancellationToken cancellationToken);

    Task<Result<ProductResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<ProductResponse>> CreateAsync(CreateProductRequest request, CancellationToken cancellationToken);

    Task<Result<ProductResponse>> UpdateAsync(Guid id, UpdateProductRequest request, CancellationToken cancellationToken);

    Task<Result<ProductResponse>> SetAvailabilityAsync(Guid id, bool isAvailable, CancellationToken cancellationToken);

    Task<Result<ProductResponse>> SetPriceAsync(Guid id, SetProductPriceRequest request, CancellationToken cancellationToken);

    Task<Result<ProductResponse>> AddExtraAsync(Guid id, AddProductExtraRequest request, CancellationToken cancellationToken);

    Task<Result<ProductResponse>> RemoveExtraAsync(Guid id, Guid extraId, CancellationToken cancellationToken);

    Task<Result<ProductResponse>> AddIngredientAsync(Guid id, AddProductIngredientRequest request, CancellationToken cancellationToken);

    Task<Result<ProductResponse>> RemoveIngredientAsync(Guid id, string ingredientName, CancellationToken cancellationToken);
}