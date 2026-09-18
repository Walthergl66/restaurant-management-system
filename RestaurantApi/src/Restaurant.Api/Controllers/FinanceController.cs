using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Finance;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers;

[ApiController]
[Route("api/finance")]
[Authorize]
public sealed class FinanceController(IFinanceService financeService) : ControllerBase
{
    private Guid CurrentUserId =>
        Guid.TryParse(User.FindFirstValue(ClaimTypes.NameIdentifier), out var id)
            ? id
            : Guid.Empty;

    [HttpGet("income")]
    [Authorize(Policy = Permissions.FinanceView)]
    [ProducesResponseType<IReadOnlyCollection<IncomeResponse>>(StatusCodes.Status200OK)]
    public async Task<IActionResult> GetIncome([FromQuery] DateTime? fromUtc, [FromQuery] DateTime? toUtc, CancellationToken cancellationToken)
    {
        var result = await financeService.GetIncomeAsync(fromUtc, toUtc, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpGet("expenses")]
    [Authorize(Policy = Permissions.FinanceView)]
    [ProducesResponseType<IReadOnlyCollection<ExpenseResponse>>(StatusCodes.Status200OK)]
    public async Task<IActionResult> GetExpenses([FromQuery] DateTime? fromUtc, [FromQuery] DateTime? toUtc, CancellationToken cancellationToken)
    {
        var result = await financeService.GetExpensesAsync(fromUtc, toUtc, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpPost("expenses")]
    [Authorize(Policy = Permissions.FinanceManage)]
    [ProducesResponseType<ExpenseResponse>(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> CreateExpense([FromBody] CreateExpenseRequest request, CancellationToken cancellationToken)
    {
        var result = await financeService.CreateExpenseAsync(request, CurrentUserId, cancellationToken);

        return result.IsSuccess
            ? StatusCode(StatusCodes.Status201Created, result.Value)
            : BadRequest(new { error = result.Error });
    }

    [HttpGet("result")]
    [Authorize(Policy = Permissions.FinanceView)]
    [ProducesResponseType<FinancialResultResponse>(StatusCodes.Status200OK)]
    public async Task<IActionResult> GetResult([FromQuery] DateTime? fromUtc, [FromQuery] DateTime? toUtc, CancellationToken cancellationToken)
    {
        var result = await financeService.GetResultAsync(fromUtc, toUtc, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }
}