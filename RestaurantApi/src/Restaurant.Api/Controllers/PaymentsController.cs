using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Payments;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers;

[ApiController]
[Route("api")]
[Authorize]
public sealed class PaymentsController(IPaymentService paymentService) : ControllerBase
{
    private Guid CurrentUserId =>
        Guid.TryParse(User.FindFirstValue(ClaimTypes.NameIdentifier), out var id)
            ? id
            : Guid.Empty;

    [HttpPost("accounts/{accountId:guid}/payments")]
    [Authorize(Policy = Permissions.PaymentsCreate)]
    [ProducesResponseType<PaymentResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> CreateForAccount(Guid accountId, [FromBody] CreateAccountPaymentRequest request, CancellationToken cancellationToken)
    {
        var result = await paymentService.CreateForAccountAsync(accountId, request, CurrentUserId, cancellationToken);

        return result.IsSuccess
            ? StatusCode(StatusCodes.Status201Created, result.Value)
            : BadRequest(new { error = result.Error });
    }

    [HttpPost("sales/{saleId:guid}/payments")]
    [Authorize(Policy = Permissions.PaymentsCreate)]
    [ProducesResponseType<PaymentResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> CreateForSale(Guid saleId, [FromBody] CreateAccountPaymentRequest request, CancellationToken cancellationToken)
    {
        var result = await paymentService.CreateForSaleAsync(saleId, request, CurrentUserId, cancellationToken);

        return result.IsSuccess
            ? StatusCode(StatusCodes.Status201Created, result.Value)
            : BadRequest(new { error = result.Error });
    }
}