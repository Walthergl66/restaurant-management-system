using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Cash;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers;

[ApiController]
[Route("api/cash")]
[Authorize]
public sealed class CashController(ICashService cashService) : ControllerBase
{
    private Guid CurrentUserId =>
        Guid.TryParse(User.FindFirstValue(ClaimTypes.NameIdentifier), out var id)
            ? id
            : Guid.Empty;

    [HttpGet("registers")]
    [Authorize(Policy = Permissions.CashOpen)]
    [ProducesResponseType<IReadOnlyCollection<CashRegisterResponse>>(StatusCodes.Status200OK)]
    public async Task<IActionResult> GetRegisters(CancellationToken cancellationToken)
    {
        var result = await cashService.GetRegistersAsync(cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpPost("registers")]
    [Authorize(Policy = Permissions.CashOpen)]
    [ProducesResponseType<CashRegisterResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> CreateRegister([FromBody] CreateCashRegisterRequest request, CancellationToken cancellationToken)
    {
        var result = await cashService.CreateRegisterAsync(request, cancellationToken);

        return result.IsSuccess
            ? CreatedAtAction(nameof(GetRegisters), result.Value)
            : BadRequest(new { error = result.Error });
    }

    [HttpPut("registers/{id:guid}")]
    [Authorize(Policy = Permissions.CashOpen)]
    [ProducesResponseType<CashRegisterResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> UpdateRegister(Guid id, [FromBody] UpdateCashRegisterRequest request, CancellationToken cancellationToken)
    {
        var result = await cashService.UpdateRegisterAsync(id, request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPost("open")]
    [Authorize(Policy = Permissions.CashOpen)]
    [ProducesResponseType<CashOpeningResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Open([FromBody] OpenCashRequest request, CancellationToken cancellationToken)
    {
        var result = await cashService.OpenAsync(request, CurrentUserId, cancellationToken);

        return result.IsSuccess
            ? CreatedAtAction(nameof(GetById), new { id = result.Value!.Id }, result.Value)
            : BadRequest(new { error = result.Error });
    }

    [HttpGet("current")]
    [Authorize(Policy = Permissions.CashOpen)]
    [ProducesResponseType<CashOpeningResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetCurrent(CancellationToken cancellationToken)
    {
        var result = await cashService.GetCurrentAsync(cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpGet("{id:guid}")]
    [Authorize(Policy = Permissions.CashOpen)]
    [ProducesResponseType<CashOpeningResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(Guid id, CancellationToken cancellationToken)
    {
        var result = await cashService.GetByIdAsync(id, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPost("{id:guid}/movements")]
    [Authorize(Policy = Permissions.CashOpen)]
    [ProducesResponseType<CashOpeningResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> RegisterMovement(Guid id, [FromBody] CashMovementRequest request, CancellationToken cancellationToken)
    {
        var result = await cashService.RegisterMovementAsync(id, request, CurrentUserId, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpPost("{id:guid}/close")]
    [Authorize(Policy = Permissions.CashClose)]
    [ProducesResponseType<CashOpeningResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Close(Guid id, [FromBody] CloseCashRequest request, CancellationToken cancellationToken)
    {
        var result = await cashService.CloseAsync(id, request, CurrentUserId, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }
}