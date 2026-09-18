using Restaurant.Application.Cash;
using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Cash;
using Restaurant.Domain.Common;

namespace Restaurant.Application.Services;

public sealed class CashService(
    ICashRegisterRepository registerRepository,
    ICashOpeningRepository openingRepository) : ICashService
{
    public async Task<Result<IReadOnlyCollection<CashRegisterResponse>>> GetRegistersAsync(CancellationToken cancellationToken)
    {
        var registers = await registerRepository.GetAllAsync(cancellationToken);

        return registers.Select(ToRegisterResponse).ToList();
    }

    public async Task<Result<CashRegisterResponse>> CreateRegisterAsync(CreateCashRegisterRequest request, CancellationToken cancellationToken)
    {
        try
        {
            var register = CashRegister.Create(request.Name, request.Description);
            await registerRepository.AddAsync(register, cancellationToken);
            await registerRepository.SaveChangesAsync(cancellationToken);

            return ToRegisterResponse(register);
        }
        catch (DomainException exception)
        {
            return Result<CashRegisterResponse>.ValidationFailure("cash.register_invalid", exception.Message);
        }
    }

    public async Task<Result<CashRegisterResponse>> UpdateRegisterAsync(Guid id, UpdateCashRegisterRequest request, CancellationToken cancellationToken)
    {
        var register = await registerRepository.GetByIdAsync(id, cancellationToken);
        if (register is null)
        {
            return Result<CashRegisterResponse>.NotFound("cash.register_not_found", "La caja no existe.");
        }

        try
        {
            register.Update(request.Name, request.Description, request.IsActive);
            await registerRepository.SaveChangesAsync(cancellationToken);

            return ToRegisterResponse(register);
        }
        catch (DomainException exception)
        {
            return Result<CashRegisterResponse>.ValidationFailure("cash.register_invalid", exception.Message);
        }
    }

    public async Task<Result<CashOpeningResponse>> OpenAsync(OpenCashRequest request, Guid userId, CancellationToken cancellationToken)
    {
        var register = await registerRepository.GetByIdAsync(request.CashRegisterId, cancellationToken);
        if (register is null)
        {
            return Result<CashOpeningResponse>.NotFound("cash.register_not_found", "La caja no existe.");
        }

        if (!register.IsActive)
        {
            return Result<CashOpeningResponse>.BusinessRuleFailure("cash.register_inactive", "La caja está inactiva.");
        }

        var existing = await openingRepository.GetOpenByRegisterAsync(request.CashRegisterId, cancellationToken);
        if (existing is not null)
        {
            return Result<CashOpeningResponse>.Conflict("cash.already_open", "La caja ya tiene una apertura activa.");
        }

        try
        {
            var opening = CashOpening.Open(request.CashRegisterId, userId, request.InitialFund);
            await openingRepository.AddAsync(opening, cancellationToken);
            await openingRepository.SaveChangesAsync(cancellationToken);

            return ToOpeningResponse(opening);
        }
        catch (DomainException exception)
        {
            return Result<CashOpeningResponse>.ValidationFailure("cash.opening_invalid", exception.Message);
        }
    }

    public async Task<Result<CashOpeningResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var opening = await openingRepository.GetWithMovementsAsync(id, cancellationToken);

        return opening is null
            ? Result<CashOpeningResponse>.NotFound("cash.opening_not_found", "La apertura de caja no existe.")
            : ToOpeningResponse(opening);
    }

    public async Task<Result<CashOpeningResponse>> GetCurrentAsync(CancellationToken cancellationToken)
    {
        var opening = await openingRepository.GetCurrentOpenAsync(cancellationToken);

        return opening is null
            ? Result<CashOpeningResponse>.NotFound("cash.no_open", "No hay una caja abierta.")
            : ToOpeningResponse(opening);
    }

    public async Task<Result<CashOpeningResponse>> RegisterMovementAsync(Guid id, CashMovementRequest request, Guid userId, CancellationToken cancellationToken)
    {
        var opening = await openingRepository.GetWithMovementsAsync(id, cancellationToken);
        if (opening is null)
        {
            return Result<CashOpeningResponse>.NotFound("cash.opening_not_found", "La apertura de caja no existe.");
        }

        if (!Enum.TryParse<CashMovementType>(request.Type, true, out var type))
        {
            return Result<CashOpeningResponse>.ValidationFailure("cash.invalid_movement_type", "El tipo de movimiento no es válido.");
        }

        try
        {
            opening.RegisterMovement(type, request.Amount, userId, request.Reference, request.Notes);
            await openingRepository.SaveChangesAsync(cancellationToken);

            return ToOpeningResponse(opening);
        }
        catch (DomainException exception)
        {
            return Result<CashOpeningResponse>.BusinessRuleFailure("cash.movement_invalid", exception.Message);
        }
    }

    public async Task<Result<CashOpeningResponse>> CloseAsync(Guid id, CloseCashRequest request, Guid userId, CancellationToken cancellationToken)
    {
        var opening = await openingRepository.GetWithMovementsAsync(id, cancellationToken);
        if (opening is null)
        {
            return Result<CashOpeningResponse>.NotFound("cash.opening_not_found", "La apertura de caja no existe.");
        }

        try
        {
            opening.Close(userId, request.CountedAmount);
            await openingRepository.SaveChangesAsync(cancellationToken);

            return ToOpeningResponse(opening);
        }
        catch (DomainException exception)
        {
            return Result<CashOpeningResponse>.BusinessRuleFailure("cash.close_invalid", exception.Message);
        }
    }

    private static CashRegisterResponse ToRegisterResponse(CashRegister register) => new(
        register.Id,
        register.Name,
        register.Description,
        register.IsActive);

    private static CashOpeningResponse ToOpeningResponse(CashOpening opening) => new(
        opening.Id,
        opening.CashRegisterId,
        opening.OpenedByUserId,
        opening.OpenedAtUtc,
        opening.InitialFund,
        opening.Status.ToString(),
        opening.ClosedByUserId,
        opening.ClosedAtUtc,
        opening.ExpectedAmount ?? opening.ComputeExpected(),
        opening.CountedAmount,
        opening.Difference,
        opening.TotalIncome,
        opening.TotalExpense,
        opening.Movements
            .Select(m => new CashMovementResponse(
                m.Id,
                m.Type.ToString(),
                m.Amount,
                m.SignedAmount,
                m.Reference,
                m.Notes,
                m.CreatedByUserId,
                m.CreatedAtUtc))
            .ToList());
}