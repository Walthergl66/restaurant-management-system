using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Tables;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers;

[ApiController]
[Route("api/accounts")]
[Authorize]
public sealed class AccountsController(ITableAccountService accountService) : ControllerBase
{
    [HttpGet]
    [Authorize(Policy = Permissions.TableAccountsManage)]
    [ProducesResponseType<IReadOnlyCollection<AccountResponse>>(StatusCodes.Status200OK)]
    public async Task<IActionResult> GetOpen(CancellationToken cancellationToken)
    {
        var result = await accountService.GetOpenAsync(cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpGet("{id:guid}")]
    [Authorize(Policy = Permissions.TableAccountsManage)]
    [ProducesResponseType<AccountResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(Guid id, CancellationToken cancellationToken)
    {
        var result = await accountService.GetByIdAsync(id, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPost]
    [Authorize(Policy = Permissions.TableAccountsManage)]
    [ProducesResponseType<AccountResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Open([FromBody] CreateAccountRequest request, CancellationToken cancellationToken)
    {
        var result = await accountService.OpenAsync(request, cancellationToken);

        return result.IsSuccess
            ? CreatedAtAction(nameof(GetById), new { id = result.Value!.Id }, result.Value)
            : BadRequest(new { error = result.Error });
    }

    [HttpPatch("{id:guid}/totals")]
    [Authorize(Policy = Permissions.TableAccountsManage)]
    [ProducesResponseType<AccountResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> UpdateTotals(Guid id, [FromBody] UpdateAccountTotalsRequest request, CancellationToken cancellationToken)
    {
        var result = await accountService.UpdateTotalsAsync(id, request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPost("{id:guid}/close")]
    [Authorize(Policy = Permissions.PaymentsCreate)]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Close(Guid id, CancellationToken cancellationToken)
    {
        var result = await accountService.CloseAccountAsync(id, cancellationToken);

        return result.IsSuccess ? NoContent() : BadRequest(new { error = result.Error });
    }
}