using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Printing;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers;

[ApiController]
[Route("api/printers")]
[Authorize]
public sealed class PrintersController(IPrinterService printerService) : ControllerBase
{
    [HttpGet]
    [Authorize(Policy = Permissions.PrinterManage)]
    [ProducesResponseType<IReadOnlyCollection<PrinterResponse>>(StatusCodes.Status200OK)]
    public async Task<IActionResult> GetActive(CancellationToken cancellationToken)
    {
        var result = await printerService.GetActiveAsync(cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpGet("{id:guid}")]
    [Authorize(Policy = Permissions.PrinterManage)]
    [ProducesResponseType<PrinterResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(Guid id, CancellationToken cancellationToken)
    {
        var result = await printerService.GetByIdAsync(id, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPost]
    [Authorize(Policy = Permissions.PrinterManage)]
    [ProducesResponseType<PrinterResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Create([FromBody] CreatePrinterRequest request, CancellationToken cancellationToken)
    {
        var result = await printerService.CreateAsync(request, cancellationToken);

        return result.IsSuccess
            ? CreatedAtAction(nameof(GetById), new { id = result.Value!.Id }, result.Value)
            : BadRequest(new { error = result.Error });
    }

    [HttpPut("{id:guid}")]
    [Authorize(Policy = Permissions.PrinterManage)]
    [ProducesResponseType<PrinterResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Update(Guid id, [FromBody] UpdatePrinterRequest request, CancellationToken cancellationToken)
    {
        var result = await printerService.UpdateAsync(id, request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPost("{id:guid}/areas/{preparationAreaId:guid}")]
    [Authorize(Policy = Permissions.PrinterManage)]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public async Task<IActionResult> AssignArea(Guid id, Guid preparationAreaId, CancellationToken cancellationToken)
    {
        var result = await printerService.AssignAreaAsync(id, preparationAreaId, cancellationToken);

        return result.IsSuccess ? NoContent() : NotFound(new { error = result.Error });
    }

    [HttpDelete("{id:guid}/areas/{preparationAreaId:guid}")]
    [Authorize(Policy = Permissions.PrinterManage)]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public async Task<IActionResult> UnassignArea(Guid id, Guid preparationAreaId, CancellationToken cancellationToken)
    {
        var result = await printerService.UnassignAreaAsync(id, preparationAreaId, cancellationToken);

        return result.IsSuccess ? NoContent() : NotFound(new { error = result.Error });
    }
}