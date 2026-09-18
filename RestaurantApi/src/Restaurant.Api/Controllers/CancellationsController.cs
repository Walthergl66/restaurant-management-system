using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Cancellations;
using Restaurant.Application.Services;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers;

[ApiController]
[Route("api")]
[Authorize]
public sealed class CancellationsController(ICancellationService cancellationService) : ControllerBase
{
    private Guid CurrentUserId =>
        Guid.TryParse(User.FindFirstValue(ClaimTypes.NameIdentifier), out var id)
            ? id
            : Guid.Empty;

    [HttpPost("orders/{orderId:guid}/cancellation-requests")]
    [Authorize(Policy = Permissions.CancellationsRequest)]
    [ProducesResponseType<CancellationRequestResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Create(Guid orderId, [FromBody] CreateCancellationRequestRequest request, CancellationToken cancellationToken)
    {
        var result = await cancellationService.CreateAsync(orderId, request, CurrentUserId, cancellationToken);

        return result.IsSuccess
            ? CreatedAtAction(nameof(GetById), new { id = result.Value!.Id }, result.Value)
            : BadRequest(new { error = result.Error });
    }

    [HttpGet("cancellation-requests/{id:guid}")]
    [Authorize(Policy = Permissions.CancellationsApprove)]
    [ProducesResponseType<CancellationRequestResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(Guid id, CancellationToken cancellationToken)
    {
        var result = await cancellationService.GetByIdAsync(id, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpGet("cancellation-requests")]
    [Authorize(Policy = Permissions.CancellationsApprove)]
    [ProducesResponseType<IReadOnlyCollection<CancellationRequestResponse>>(StatusCodes.Status200OK)]
    public async Task<IActionResult> GetPending(CancellationToken cancellationToken)
    {
        var result = await cancellationService.GetPendingAsync(cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpPost("cancellation-requests/{id:guid}/approve")]
    [Authorize(Policy = Permissions.CancellationsApprove)]
    [ProducesResponseType<CancellationRequestResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Approve(Guid id, [FromBody] ReviewCancellationRequestRequest request, CancellationToken cancellationToken)
    {
        var result = await cancellationService.ApproveAsync(id, request, CurrentUserId, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpPost("cancellation-requests/{id:guid}/reject")]
    [Authorize(Policy = Permissions.CancellationsApprove)]
    [ProducesResponseType<CancellationRequestResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Reject(Guid id, [FromBody] ReviewCancellationRequestRequest request, CancellationToken cancellationToken)
    {
        var result = await cancellationService.RejectAsync(id, request, CurrentUserId, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }
}