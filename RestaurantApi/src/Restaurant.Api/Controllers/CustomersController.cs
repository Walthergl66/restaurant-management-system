using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Customers;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers;

[ApiController]
[Route("api/customers")]
[Authorize]
public sealed class CustomersController(ICustomerService customerService) : ControllerBase
{
    [HttpGet("{id:guid}")]
    [Authorize(Policy = Permissions.CatalogView)]
    [ProducesResponseType<CustomerProfileResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(Guid id, CancellationToken cancellationToken)
    {
        var result = await customerService.GetByIdAsync(id, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpGet("by-user/{userId:guid}")]
    [Authorize(Policy = Permissions.CatalogView)]
    [ProducesResponseType<CustomerProfileResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetByUser(Guid userId, CancellationToken cancellationToken)
    {
        var result = await customerService.GetByUserAsync(userId, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPost]
    [Authorize(Policy = Permissions.TableAccountsManage)]
    [ProducesResponseType<CustomerProfileResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Create([FromBody] CreateCustomerProfileRequest request, CancellationToken cancellationToken)
    {
        var result = await customerService.CreateAsync(request, cancellationToken);

        return result.IsSuccess
            ? CreatedAtAction(nameof(GetById), new { id = result.Value!.Id }, result.Value)
            : BadRequest(new { error = result.Error });
    }

    [HttpPut("{id:guid}")]
    [Authorize(Policy = Permissions.TableAccountsManage)]
    [ProducesResponseType<CustomerProfileResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Update(Guid id, [FromBody] UpdateCustomerProfileRequest request, CancellationToken cancellationToken)
    {
        var result = await customerService.UpdateAsync(id, request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : NotFound(new { error = result.Error });
    }

    [HttpPost("{id:guid}/addresses")]
    [Authorize(Policy = Permissions.TableAccountsManage)]
    [ProducesResponseType<CustomerProfileResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> AddAddress(Guid id, [FromBody] CreateAddressRequest request, CancellationToken cancellationToken)
    {
        var result = await customerService.AddAddressAsync(id, request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpDelete("{id:guid}/addresses/{addressId:guid}")]
    [Authorize(Policy = Permissions.TableAccountsManage)]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> RemoveAddress(Guid id, Guid addressId, CancellationToken cancellationToken)
    {
        var result = await customerService.RemoveAddressAsync(id, addressId, cancellationToken);

        return result.IsSuccess ? NoContent() : BadRequest(new { error = result.Error });
    }
}