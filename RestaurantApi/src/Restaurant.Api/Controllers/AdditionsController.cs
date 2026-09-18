using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Additions;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers;

[ApiController]
[Route("api/table-accounts/{accountId:guid}/additions")]
[Authorize]
public sealed class AdditionsController(IAdditionService additionService) : ControllerBase
{
    private Guid CurrentUserId =>
        Guid.TryParse(User.FindFirstValue(ClaimTypes.NameIdentifier), out var id)
            ? id
            : Guid.Empty;

    [HttpGet("{id:guid}")]
    [Authorize(Policy = Permissions.AdditionsCreate)]
    [ProducesResponseType<AdditionResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(Guid id, CancellationToken cancellationToken)
    {
        var result = await additionService.GetByIdAsync(id, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPost]
    [Authorize(Policy = Permissions.AdditionsCreate)]
    [ProducesResponseType<AdditionResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Create(Guid accountId, [FromBody] CreateAdditionRequest request, CancellationToken cancellationToken)
    {
        var result = await additionService.CreateAsync(request, CurrentUserId, cancellationToken);

        return result.IsSuccess
            ? CreatedAtAction(nameof(GetById), new { accountId, id = result.Value!.Id }, result.Value)
            : BadRequest(new { error = result.Error });
    }

    [HttpPost("{id:guid}/items")]
    [Authorize(Policy = Permissions.AdditionsCreate)]
    [ProducesResponseType<AdditionResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> AddItem(Guid id, [FromBody] CreateAdditionItemRequest request, CancellationToken cancellationToken)
    {
        var result = await additionService.AddItemAsync(id, request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpDelete("{id:guid}/items/{itemId:guid}")]
    [Authorize(Policy = Permissions.AdditionsCreate)]
    [ProducesResponseType<AdditionResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> RemoveItem(Guid id, Guid itemId, CancellationToken cancellationToken)
    {
        var result = await additionService.RemoveItemAsync(id, itemId, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpPost("{id:guid}/confirm")]
    [Authorize(Policy = Permissions.AdditionsCreate)]
    [ProducesResponseType<AdditionResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Confirm(Guid id, CancellationToken cancellationToken)
    {
        var result = await additionService.ConfirmAsync(id, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }
}