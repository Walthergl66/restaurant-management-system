using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Common;
using Restaurant.Domain.Catalog;

namespace Restaurant.Application.Catalog;

public sealed class ExtraService(IExtraRepository extraRepository) : IExtraService
{
    public async Task<Result<IReadOnlyCollection<ExtraResponse>>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken)
    {
        var extras = await extraRepository.GetAllAsync(onlyActive, cancellationToken);

        return extras
            .Select(ToResponse)
            .ToList();
    }

    public async Task<Result<ExtraResponse>> CreateAsync(CreateExtraRequest request, CancellationToken cancellationToken)
    {
        try
        {
            var extra = Extra.Create(request.Name, request.Price);
            await extraRepository.AddAsync(extra, cancellationToken);
            await extraRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(extra);
        }
        catch (DomainException exception)
        {
            return Result<ExtraResponse>.ValidationFailure("extra.invalid", exception.Message);
        }
    }

    public async Task<Result<ExtraResponse>> UpdateAsync(Guid id, UpdateExtraRequest request, CancellationToken cancellationToken)
    {
        var extra = await extraRepository.GetByIdAsync(id, cancellationToken);
        if (extra is null)
        {
            return Result<ExtraResponse>.NotFound("extra.not_found", "El extra no existe.");
        }

        try
        {
            extra.Update(request.Name, request.Price, request.IsActive);
            await extraRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(extra);
        }
        catch (DomainException exception)
        {
            return Result<ExtraResponse>.ValidationFailure("extra.invalid", exception.Message);
        }
    }

    private static ExtraResponse ToResponse(Extra extra) => new(
        extra.Id,
        extra.Name,
        extra.Price,
        extra.IsActive);
}