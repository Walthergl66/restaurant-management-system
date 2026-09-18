namespace Restaurant.Application.Catalog;

public sealed record CategoryResponse(
    Guid Id,
    string Name,
    string? Description,
    bool IsActive,
    int SortOrder);

public sealed record CreateCategoryRequest(string Name, string? Description, int SortOrder = 0);

public sealed record UpdateCategoryRequest(string Name, string? Description, int SortOrder, bool IsActive);

public sealed record PreparationAreaResponse(Guid Id, string Name, string? Description, bool IsActive);

public sealed record CreatePreparationAreaRequest(string Name, string? Description);

public sealed record UpdatePreparationAreaRequest(string Name, string? Description, bool IsActive);

public sealed record ExtraResponse(Guid Id, string Name, decimal Price, bool IsActive);

public sealed record CreateExtraRequest(string Name, decimal Price);

public sealed record UpdateExtraRequest(string Name, decimal Price, bool IsActive);

public sealed record ProductPriceResponse(Guid Id, decimal Price, DateTime EffectiveFrom);

public sealed record ProductExtraResponse(Guid ExtraId, string Name, decimal Price, bool IsRequired, int? MaxQuantity);

public sealed record ProductIngredientResponse(Guid Id, string IngredientName, bool IsRemovable);

public sealed record ProductResponse(
    Guid Id,
    string Name,
    string? Description,
    Guid CategoryId,
    Guid PreparationAreaId,
    bool IsAvailable,
    decimal CurrentPrice,
    IReadOnlyCollection<ProductPriceResponse> Prices,
    IReadOnlyCollection<ProductExtraResponse> AvailableExtras,
    IReadOnlyCollection<ProductIngredientResponse> Ingredients);

public sealed record CreateProductRequest(
    string Name,
    string? Description,
    Guid CategoryId,
    Guid PreparationAreaId,
    decimal Price,
    bool IsAvailable = true);

public sealed record UpdateProductRequest(
    string Name,
    string? Description,
    Guid CategoryId,
    Guid PreparationAreaId,
    decimal Price,
    bool IsAvailable);

public sealed record SetProductPriceRequest(decimal Price, DateTime EffectiveFrom);

public sealed record AddProductExtraRequest(Guid ExtraId, bool IsRequired = false, int? MaxQuantity = null);

public sealed record AddProductIngredientRequest(string IngredientName, bool IsRemovable = true);