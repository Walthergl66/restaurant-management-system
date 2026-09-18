using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Payments;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers;

[ApiController]
[Route("api/sales")]
[Authorize]
public sealed class SalesController(ISaleService saleService) : ControllerBase
{
    [HttpGet("{id:guid}")]
    [Authorize(Policy = Permissions.PaymentsCreate)]
    [ProducesResponseType<SaleResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(Guid id, CancellationToken cancellationToken)
    {
        var result = await saleService.GetByIdAsync(id, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpGet("by-account/{accountId:guid}")]
    [Authorize(Policy = Permissions.PaymentsCreate)]
    [ProducesResponseType<SaleResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetByAccount(Guid accountId, CancellationToken cancellationToken)
    {
        var result = await saleService.GetByAccountAsync(accountId, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPost]
    [Authorize(Policy = Permissions.PaymentsCreate)]
    [ProducesResponseType<SaleResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Create([FromBody] CreateSaleRequest request, CancellationToken cancellationToken)
    {
        var result = await saleService.CreateAsync(request, cancellationToken);

        return result.IsSuccess
            ? CreatedAtAction(nameof(GetById), new { id = result.Value!.Id }, result.Value)
            : BadRequest(new { error = result.Error });
    }

    [HttpPost("{saleId:guid}/items")]
    [Authorize(Policy = Permissions.PaymentsCreate)]
    [ProducesResponseType<SaleResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> AddItem(Guid saleId, [FromBody] SaleItemRequest request, CancellationToken cancellationToken)
    {
        var result = await saleService.AddItemAsync(saleId, request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }
}