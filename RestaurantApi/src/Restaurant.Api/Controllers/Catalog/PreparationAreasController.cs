using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Catalog;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers.Catalog;

[ApiController]
[Route("api/catalog/preparation-areas")]
[Authorize]
public sealed class PreparationAreasController(IPreparationAreaService preparationAreaService) : ControllerBase
{
    [HttpGet]
    [Authorize(Policy = Permissions.CatalogView)]
    [ProducesResponseType<IReadOnlyCollection<PreparationAreaResponse>>(StatusCodes.Status200OK)]
    public async Task<IActionResult> GetAll([FromQuery] bool onlyActive = false, CancellationToken cancellationToken = default)
    {
        var result = await preparationAreaService.GetAllAsync(onlyActive, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpGet("{id:guid}")]
    [Authorize(Policy = Permissions.CatalogView)]
    [ProducesResponseType<PreparationAreaResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(Guid id, CancellationToken cancellationToken)
    {
        var result = await preparationAreaService.GetByIdAsync(id, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPost]
    [Authorize(Policy = Permissions.CatalogManage)]
    [ProducesResponseType<PreparationAreaResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Create([FromBody] CreatePreparationAreaRequest request, CancellationToken cancellationToken)
    {
        var result = await preparationAreaService.CreateAsync(request, cancellationToken);

        return result.IsSuccess
            ? CreatedAtAction(nameof(GetById), new { id = result.Value!.Id }, result.Value)
            : BadRequest(new { error = result.Error });
    }

    [HttpPut("{id:guid}")]
    [Authorize(Policy = Permissions.CatalogManage)]
    [ProducesResponseType<PreparationAreaResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Update(Guid id, [FromBody] UpdatePreparationAreaRequest request, CancellationToken cancellationToken)
    {
        var result = await preparationAreaService.UpdateAsync(id, request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }
}