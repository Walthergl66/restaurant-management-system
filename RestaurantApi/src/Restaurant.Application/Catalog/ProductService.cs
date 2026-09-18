using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Common;
using Restaurant.Domain.Catalog;

namespace Restaurant.Application.Catalog;

public sealed class ProductService(
    IProductRepository productRepository,
    ICategoryRepository categoryRepository,
    IPreparationAreaRepository preparationAreaRepository,
    IExtraRepository extraRepository) : IProductService
{
    public async Task<Result<IReadOnlyCollection<ProductResponse>>> GetAllAsync(bool onlyAvailable, CancellationToken cancellationToken)
    {
        var products = await productRepository.GetAllAsync(onlyAvailable, cancellationToken);

        var responses = new List<ProductResponse>();
        foreach (var product in products)
        {
            responses.Add(ToResponse(product));
        }

        return responses;
    }

    public async Task<Result<ProductResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var product = await productRepository.GetWithDetailsAsync(id, cancellationToken);

        return product is null
            ? Result<ProductResponse>.NotFound("product.not_found", "El producto no existe.")
            : ToResponse(product);
    }

    public async Task<Result<ProductResponse>> CreateAsync(CreateProductRequest request, CancellationToken cancellationToken)
    {
        var category = await categoryRepository.GetByIdAsync(request.CategoryId, cancellationToken);
        if (category is null)
        {
            return Result<ProductResponse>.ValidationFailure("product.category_not_found", "La categoría indicada no existe.");
        }

        var area = await preparationAreaRepository.GetByIdAsync(request.PreparationAreaId, cancellationToken);
        if (area is null)
        {
            return Result<ProductResponse>.ValidationFailure("product.area_not_found", "El área de preparación indicada no existe.");
        }

        try
        {
            var product = Product.Create(request.Name, request.CategoryId, request.PreparationAreaId, request.Description, request.IsAvailable);
            product.SetPrice(request.Price, DateTime.UtcNow.Date);

            await productRepository.AddAsync(product, cancellationToken);
            await productRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(product);
        }
        catch (DomainException exception)
        {
            return Result<ProductResponse>.ValidationFailure("product.invalid", exception.Message);
        }
    }

    public async Task<Result<ProductResponse>> UpdateAsync(Guid id, UpdateProductRequest request, CancellationToken cancellationToken)
    {
        var product = await productRepository.GetWithDetailsAsync(id, cancellationToken);
        if (product is null)
        {
            return Result<ProductResponse>.NotFound("product.not_found", "El producto no existe.");
        }

        var category = await categoryRepository.GetByIdAsync(request.CategoryId, cancellationToken);
        if (category is null)
        {
            return Result<ProductResponse>.ValidationFailure("product.category_not_found", "La categoría indicada no existe.");
        }

        var area = await preparationAreaRepository.GetByIdAsync(request.PreparationAreaId, cancellationToken);
        if (area is null)
        {
            return Result<ProductResponse>.ValidationFailure("product.area_not_found", "El área de preparación indicada no existe.");
        }

        try
        {
            product.Update(request.Name, request.CategoryId, request.PreparationAreaId, request.Description, request.IsAvailable);
            await productRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(product);
        }
        catch (DomainException exception)
        {
            return Result<ProductResponse>.ValidationFailure("product.invalid", exception.Message);
        }
    }

    public async Task<Result<ProductResponse>> SetAvailabilityAsync(Guid id, bool isAvailable, CancellationToken cancellationToken)
    {
        var product = await productRepository.GetWithDetailsAsync(id, cancellationToken);
        if (product is null)
        {
            return Result<ProductResponse>.NotFound("product.not_found", "El producto no existe.");
        }

        product.SetAvailability(isAvailable);
        await productRepository.SaveChangesAsync(cancellationToken);

        return ToResponse(product);
    }

    public async Task<Result<ProductResponse>> SetPriceAsync(Guid id, SetProductPriceRequest request, CancellationToken cancellationToken)
    {
        var product = await productRepository.GetWithDetailsAsync(id, cancellationToken);
        if (product is null)
        {
            return Result<ProductResponse>.NotFound("product.not_found", "El producto no existe.");
        }

        try
        {
            product.SetPrice(request.Price, request.EffectiveFrom.ToUniversalTime());
            await productRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(product);
        }
        catch (DomainException exception)
        {
            return Result<ProductResponse>.ValidationFailure("product.invalid_price", exception.Message);
        }
    }

    public async Task<Result<ProductResponse>> AddExtraAsync(Guid id, AddProductExtraRequest request, CancellationToken cancellationToken)
    {
        var product = await productRepository.GetWithDetailsAsync(id, cancellationToken);
        if (product is null)
        {
            return Result<ProductResponse>.NotFound("product.not_found", "El producto no existe.");
        }

        var extra = await extraRepository.GetByIdAsync(request.ExtraId, cancellationToken);
        if (extra is null)
        {
            return Result<ProductResponse>.ValidationFailure("product.extra_not_found", "El extra indicado no existe.");
        }

        product.AddExtra(ProductExtra.Create(id, request.ExtraId, request.IsRequired, request.MaxQuantity));
        await productRepository.SaveChangesAsync(cancellationToken);

        return ToResponse(product);
    }

    public async Task<Result<ProductResponse>> RemoveExtraAsync(Guid id, Guid extraId, CancellationToken cancellationToken)
    {
        var product = await productRepository.GetWithDetailsAsync(id, cancellationToken);
        if (product is null)
        {
            return Result<ProductResponse>.NotFound("product.not_found", "El producto no existe.");
        }

        product.RemoveExtra(extraId);
        await productRepository.SaveChangesAsync(cancellationToken);

        return ToResponse(product);
    }

    public async Task<Result<ProductResponse>> AddIngredientAsync(Guid id, AddProductIngredientRequest request, CancellationToken cancellationToken)
    {
        var product = await productRepository.GetWithDetailsAsync(id, cancellationToken);
        if (product is null)
        {
            return Result<ProductResponse>.NotFound("product.not_found", "El producto no existe.");
        }

        try
        {
            product.AddIngredient(ProductIngredient.Create(id, request.IngredientName, request.IsRemovable));
            await productRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(product);
        }
        catch (DomainException exception)
        {
            return Result<ProductResponse>.ValidationFailure("product.invalid_ingredient", exception.Message);
        }
    }

    public async Task<Result<ProductResponse>> RemoveIngredientAsync(Guid id, string ingredientName, CancellationToken cancellationToken)
    {
        var product = await productRepository.GetWithDetailsAsync(id, cancellationToken);
        if (product is null)
        {
            return Result<ProductResponse>.NotFound("product.not_found", "El producto no existe.");
        }

        product.RemoveIngredient(ingredientName);
        await productRepository.SaveChangesAsync(cancellationToken);

        return ToResponse(product);
    }

    private static ProductResponse ToResponse(Product product)
    {
        var currentPrice = product.Prices
            .Where(p => p.EffectiveFrom <= DateTime.UtcNow)
            .OrderByDescending(p => p.EffectiveFrom)
            .Select(p => p.Price)
            .FirstOrDefault();

        return new ProductResponse(
            product.Id,
            product.Name,
            product.Description,
            product.CategoryId,
            product.PreparationAreaId,
            product.IsAvailable,
            currentPrice,
            product.Prices
                .OrderByDescending(p => p.EffectiveFrom)
                .Select(p => new ProductPriceResponse(p.Id, p.Price, p.EffectiveFrom))
                .ToList(),
            product.AvailableExtras
                .Select(e => new ProductExtraResponse(
                    e.ExtraId,
                    e.Extra?.Name ?? "Extra",
                    e.Extra?.Price ?? 0,
                    e.IsRequired,
                    e.MaxQuantity))
                .ToList(),
            product.Ingredients
                .Select(i => new ProductIngredientResponse(i.Id, i.IngredientName, i.IsRemovable))
                .ToList());
    }
}