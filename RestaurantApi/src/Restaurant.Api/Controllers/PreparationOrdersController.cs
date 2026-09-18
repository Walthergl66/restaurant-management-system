using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Preparation;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers;

[ApiController]
[Route("api/preparation")]
[Authorize]
public sealed class PreparationOrdersController(IPreparationOrderService preparationOrderService) : ControllerBase
{
    [HttpGet]
    [Authorize(Policy = Permissions.OrdersConfirm)]
    [ProducesResponseType<IReadOnlyCollection<PreparationOrderResponse>>(StatusCodes.Status200OK)]
    public async Task<IActionResult> GetByStatus([FromQuery] int status, CancellationToken cancellationToken)
    {
        var result = await preparationOrderService.GetByStatusAsync(status, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpGet("{id:guid}")]
    [Authorize(Policy = Permissions.OrdersConfirm)]
    [ProducesResponseType<PreparationOrderResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(Guid id, CancellationToken cancellationToken)
    {
        var result = await preparationOrderService.GetByIdAsync(id, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPost("{id:guid}/start")]
    [Authorize(Policy = Permissions.OrdersConfirm)]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Start(Guid id, CancellationToken cancellationToken)
    {
        var result = await preparationOrderService.StartAsync(id, cancellationToken);

        return result.IsSuccess ? NoContent() : BadRequest(new { error = result.Error });
    }

    [HttpPost("{id:guid}/ready")]
    [Authorize(Policy = Permissions.OrdersConfirm)]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public async Task<IActionResult> MarkReady(Guid id, CancellationToken cancellationToken)
    {
        var result = await preparationOrderService.MarkReadyAsync(id, cancellationToken);

        return result.IsSuccess ? NoContent() : BadRequest(new { error = result.Error });
    }

    [HttpPost("{id:guid}/cancel")]
    [Authorize(Policy = Permissions.OrdersCancel)]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public async Task<IActionResult> Cancel(Guid id, CancellationToken cancellationToken)
    {
        var result = await preparationOrderService.CancelAsync(id, cancellationToken);

        return result.IsSuccess ? NoContent() : BadRequest(new { error = result.Error });
    }
}