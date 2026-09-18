using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Tables;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers;

[ApiController]
[Route("api/tables")]
[Authorize]
public sealed class TablesController(ITableService tableService) : ControllerBase
{
    [HttpGet]
    [Authorize(Policy = Permissions.TableAccountsManage)]
    [ProducesResponseType<IReadOnlyCollection<TableResponse>>(StatusCodes.Status200OK)]
    public async Task<IActionResult> GetAll([FromQuery] bool onlyAvailable = false, CancellationToken cancellationToken = default)
    {
        var result = await tableService.GetAllAsync(onlyAvailable, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpGet("{id:guid}")]
    [Authorize(Policy = Permissions.TableAccountsManage)]
    [ProducesResponseType<TableResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(Guid id, CancellationToken cancellationToken)
    {
        var result = await tableService.GetByIdAsync(id, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPost]
    [Authorize(Policy = Permissions.TableAccountsManage)]
    [ProducesResponseType<TableResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Create([FromBody] CreateTableRequest request, CancellationToken cancellationToken)
    {
        var result = await tableService.CreateAsync(request, cancellationToken);

        return result.IsSuccess
            ? CreatedAtAction(nameof(GetById), new { id = result.Value!.Id }, result.Value)
            : BadRequest(new { error = result.Error });
    }

    [HttpPut("{id:guid}")]
    [Authorize(Policy = Permissions.TableAccountsManage)]
    [ProducesResponseType<TableResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Update(Guid id, [FromBody] UpdateTableRequest request, CancellationToken cancellationToken)
    {
        var result = await tableService.UpdateAsync(id, request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPatch("{id:guid}/status")]
    [Authorize(Policy = Permissions.TableAccountsManage)]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> SetStatus(Guid id, [FromBody] int tableStatus, CancellationToken cancellationToken)
    {
        var result = await tableService.SetStatusAsync(id, tableStatus, cancellationToken);

        return result.IsSuccess ? NoContent() : BadRequest(new { error = result.Error });
    }
}