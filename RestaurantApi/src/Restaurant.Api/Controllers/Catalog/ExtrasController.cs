using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Catalog;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers.Catalog;

[ApiController]
[Route("api/catalog/extras")]
[Authorize]
public sealed class ExtrasController(IExtraService extraService) : ControllerBase
{
    [HttpGet]
    [Authorize(Policy = Permissions.CatalogView)]
    [ProducesResponseType<IReadOnlyCollection<ExtraResponse>>(StatusCodes.Status200OK)]
    public async Task<IActionResult> GetAll([FromQuery] bool onlyActive = false, CancellationToken cancellationToken = default)
    {
        var result = await extraService.GetAllAsync(onlyActive, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpPost]
    [Authorize(Policy = Permissions.CatalogManage)]
    [ProducesResponseType<ExtraResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Create([FromBody] CreateExtraRequest request, CancellationToken cancellationToken)
    {
        var result = await extraService.CreateAsync(request, cancellationToken);

        return result.IsSuccess
            ? CreatedAtAction(nameof(GetAll), new { id = result.Value!.Id }, result.Value)
            : BadRequest(new { error = result.Error });
    }

    [HttpPut("{id:guid}")]
    [Authorize(Policy = Permissions.CatalogManage)]
    [ProducesResponseType<ExtraResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Update(Guid id, [FromBody] UpdateExtraRequest request, CancellationToken cancellationToken)
    {
        var result = await extraService.UpdateAsync(id, request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }
}