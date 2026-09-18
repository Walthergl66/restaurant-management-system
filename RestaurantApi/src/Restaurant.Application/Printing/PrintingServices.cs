using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Common;
using Restaurant.Domain.Printing;

namespace Restaurant.Application.Printing;

public sealed class PrinterService(IPrinterRepository repository) : IPrinterService
{
    public async Task<Result<IReadOnlyCollection<PrinterResponse>>> GetActiveAsync(CancellationToken cancellationToken)
    {
        var printers = await repository.GetActiveAsync(cancellationToken);

        return printers.Select(ToResponse).ToList();
    }

    public async Task<Result<PrinterResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var printer = await repository.GetByIdAsync(id, cancellationToken);

        return printer is null
            ? Result<PrinterResponse>.NotFound("printer.not_found", "La impresora no existe.")
            : ToResponse(printer);
    }

    public async Task<Result<PrinterResponse>> CreateAsync(CreatePrinterRequest request, CancellationToken cancellationToken)
    {
        try
        {
            var printer = Printer.Create(request.Name, request.Description, request.IpAddress, request.Port);
            await repository.AddAsync(printer, cancellationToken);
            await repository.SaveChangesAsync(cancellationToken);

            return ToResponse(printer);
        }
        catch (DomainException exception)
        {
            return Result<PrinterResponse>.ValidationFailure("printer.invalid", exception.Message);
        }
    }

    public async Task<Result<PrinterResponse>> UpdateAsync(Guid id, UpdatePrinterRequest request, CancellationToken cancellationToken)
    {
        var printer = await repository.GetByIdAsync(id, cancellationToken);
        if (printer is null)
        {
            return Result<PrinterResponse>.NotFound("printer.not_found", "La impresora no existe.");
        }

        try
        {
            printer.Update(request.Name, request.Description, request.IpAddress, request.Port, request.IsActive);
            await repository.SaveChangesAsync(cancellationToken);

            return ToResponse(printer);
        }
        catch (DomainException exception)
        {
            return Result<PrinterResponse>.ValidationFailure("printer.invalid", exception.Message);
        }
    }

    public async Task<Result> AssignAreaAsync(Guid id, Guid preparationAreaId, CancellationToken cancellationToken)
    {
        var printer = await repository.GetByIdAsync(id, cancellationToken);
        if (printer is null)
        {
            return Result.NotFound("printer.not_found", "La impresora no existe.");
        }

        printer.AssignToArea(preparationAreaId);
        await repository.SaveChangesAsync(cancellationToken);

        return Result.Success();
    }

    public async Task<Result> UnassignAreaAsync(Guid id, Guid preparationAreaId, CancellationToken cancellationToken)
    {
        var printer = await repository.GetByIdAsync(id, cancellationToken);
        if (printer is null)
        {
            return Result.NotFound("printer.not_found", "La impresora no existe.");
        }

        printer.UnassignFromArea(preparationAreaId);
        await repository.SaveChangesAsync(cancellationToken);

        return Result.Success();
    }

    public static PrinterResponse ToResponse(Printer printer) => new(
        printer.Id,
        printer.Name,
        printer.Description,
        printer.IpAddress,
        printer.Port,
        printer.IsActive);
}

public sealed class PrintJobService(IPrintJobRepository repository) : IPrintJobService
{
    public async Task<Result<PrintJobResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var printJob = await repository.GetByIdAsync(id, cancellationToken);

        return printJob is null
            ? Result<PrintJobResponse>.NotFound("printjob.not_found", "El trabajo de impresión no existe.")
            : ToResponse(printJob);
    }

    public async Task<Result<PrintJobResponse>> CreateAsync(
        Guid? sourceOrderId,
        Guid? preparationOrderId,
        Guid printerId,
        string idempotencyKey,
        CancellationToken cancellationToken)
    {
        var existing = await repository.GetByIdempotencyKeyAsync(idempotencyKey, cancellationToken);
        if (existing is not null)
        {
            return ToResponse(existing);
        }

        try
        {
            var printJob = PrintJob.Create(sourceOrderId, preparationOrderId, printerId, idempotencyKey);
            await repository.AddAsync(printJob, cancellationToken);
            await repository.SaveChangesAsync(cancellationToken);

            return ToResponse(printJob);
        }
        catch (DomainException exception)
        {
            return Result<PrintJobResponse>.ValidationFailure("printjob.invalid", exception.Message);
        }
    }

    public async Task<Result> MarkSuccessAsync(Guid id, CancellationToken cancellationToken)
    {
        var printJob = await repository.GetByIdAsync(id, cancellationToken);
        if (printJob is null)
        {
            return Result.NotFound("printjob.not_found", "El trabajo de impresión no existe.");
        }

        printJob.MarkSuccess();
        await repository.SaveChangesAsync(cancellationToken);

        return Result.Success();
    }

    public async Task<Result> MarkFailedAsync(Guid id, string? error, CancellationToken cancellationToken)
    {
        var printJob = await repository.GetByIdAsync(id, cancellationToken);
        if (printJob is null)
        {
            return Result.NotFound("printjob.not_found", "El trabajo de impresión no existe.");
        }

        printJob.MarkFailed(error);
        await repository.SaveChangesAsync(cancellationToken);

        return Result.Success();
    }

    public static PrintJobResponse ToResponse(PrintJob printJob) => new(
        printJob.Id,
        printJob.SourceOrderId,
        printJob.PreparationOrderId,
        printJob.PrinterId,
        printJob.Status.ToString(),
        printJob.Attempts,
        printJob.IdempotencyKey,
        printJob.Error,
        printJob.CompletedAtUtc);
}