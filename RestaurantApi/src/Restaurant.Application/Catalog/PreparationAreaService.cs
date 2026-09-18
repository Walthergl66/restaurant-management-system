using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Common;
using Restaurant.Domain.Catalog;

namespace Restaurant.Application.Catalog;

public sealed class PreparationAreaService(IPreparationAreaRepository preparationAreaRepository) : IPreparationAreaService
{
    public async Task<Result<IReadOnlyCollection<PreparationAreaResponse>>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken)
    {
        var areas = await preparationAreaRepository.GetAllAsync(onlyActive, cancellationToken);

        return areas
            .Select(ToResponse)
            .ToList();
    }

    public async Task<Result<PreparationAreaResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var area = await preparationAreaRepository.GetByIdAsync(id, cancellationToken);

        return area is null
            ? Result<PreparationAreaResponse>.NotFound("area.not_found", "El área de preparación no existe.")
            : ToResponse(area);
    }

    public async Task<Result<PreparationAreaResponse>> CreateAsync(CreatePreparationAreaRequest request, CancellationToken cancellationToken)
    {
        try
        {
            var area = PreparationArea.Create(request.Name, request.Description);
            await preparationAreaRepository.AddAsync(area, cancellationToken);
            await preparationAreaRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(area);
        }
        catch (DomainException exception)
        {
            return Result<PreparationAreaResponse>.ValidationFailure("area.invalid", exception.Message);
        }
    }

    public async Task<Result<PreparationAreaResponse>> UpdateAsync(Guid id, UpdatePreparationAreaRequest request, CancellationToken cancellationToken)
    {
        var area = await preparationAreaRepository.GetByIdAsync(id, cancellationToken);
        if (area is null)
        {
            return Result<PreparationAreaResponse>.NotFound("area.not_found", "El área de preparación no existe.");
        }

        try
        {
            area.Update(request.Name, request.Description, request.IsActive);
            await preparationAreaRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(area);
        }
        catch (DomainException exception)
        {
            return Result<PreparationAreaResponse>.ValidationFailure("area.invalid", exception.Message);
        }
    }

    private static PreparationAreaResponse ToResponse(PreparationArea area) => new(
        area.Id,
        area.Name,
        area.Description,
        area.IsActive);
}