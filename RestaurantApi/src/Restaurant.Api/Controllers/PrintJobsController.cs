using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Printing;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers;

[ApiController]
[Route("api/print-jobs")]
[Authorize]
public sealed class PrintJobsController(IPrintJobService printJobService) : ControllerBase
{
    [HttpPost]
    [Authorize(Policy = Permissions.PrinterManage)]
    [ProducesResponseType<PrintJobResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Create([FromBody] CreatePrintJobRequest request, CancellationToken cancellationToken)
    {
        var result = await printJobService.CreateAsync(
            request.SourceOrderId,
            request.PreparationOrderId,
            request.PrinterId,
            request.IdempotencyKey,
            cancellationToken);

        return result.IsSuccess
            ? CreatedAtAction(nameof(GetById), new { id = result.Value!.Id }, result.Value)
            : BadRequest(new { error = result.Error });
    }

    [HttpGet("{id:guid}")]
    [Authorize(Policy = Permissions.PrinterManage)]
    [ProducesResponseType<PrintJobResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(Guid id, CancellationToken cancellationToken)
    {
        var result = await printJobService.GetByIdAsync(id, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPost("{id:guid}/success")]
    [Authorize(Policy = Permissions.PrinterManage)]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public async Task<IActionResult> MarkSuccess(Guid id, CancellationToken cancellationToken)
    {
        var result = await printJobService.MarkSuccessAsync(id, cancellationToken);

        return result.IsSuccess ? NoContent() : NotFound(new { error = result.Error });
    }

    [HttpPost("{id:guid}/failed")]
    [Authorize(Policy = Permissions.PrinterManage)]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public async Task<IActionResult> MarkFailed(Guid id, [FromBody] string? error, CancellationToken cancellationToken)
    {
        var result = await printJobService.MarkFailedAsync(id, error, cancellationToken);

        return result.IsSuccess ? NoContent() : NotFound(new { error = result.Error });
    }
}

public sealed record CreatePrintJobRequest(
    Guid? SourceOrderId,
    Guid? PreparationOrderId,
    Guid PrinterId,
    string IdempotencyKey);