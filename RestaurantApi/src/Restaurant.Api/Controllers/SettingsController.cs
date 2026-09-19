using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Settings;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers;

[ApiController]
[Route("api/settings")]
[Authorize(Policy = Permissions.SettingsManage)]
public sealed class SettingsController(ISettingsService settingsService) : ControllerBase
{
    [HttpGet]
    [ProducesResponseType<RestaurantSettingsResponse>(StatusCodes.Status200OK)]
    public async Task<IActionResult> Get(CancellationToken cancellationToken)
    {
        var result = await settingsService.GetAsync(cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpPut]
    [ProducesResponseType<RestaurantSettingsResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Update([FromBody] UpdateRestaurantSettingsRequest request, CancellationToken cancellationToken)
    {
        var result = await settingsService.UpdateAsync(request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpPut("payment")]
    [ProducesResponseType<RestaurantSettingsResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> UpdatePayment([FromBody] UpdatePaymentSettingsRequest request, CancellationToken cancellationToken)
    {
        var result = await settingsService.UpdatePaymentAsync(request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpPut("cancellation")]
    [ProducesResponseType<RestaurantSettingsResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> UpdateCancellation([FromBody] UpdateCancellationSettingsRequest request, CancellationToken cancellationToken)
    {
        var result = await settingsService.UpdateCancellationAsync(request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpGet("printers")]
    [ProducesResponseType<IReadOnlyCollection<PrinterSettingResponse>>(StatusCodes.Status200OK)]
    public async Task<IActionResult> GetPrinterSettings(CancellationToken cancellationToken)
    {
        var result = await settingsService.GetPrinterSettingsAsync(cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpPut("printers")]
    [ProducesResponseType<PrinterSettingResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> UpsertPrinterSetting([FromBody] UpsertPrinterSettingRequest request, CancellationToken cancellationToken)
    {
        var result = await settingsService.UpsertPrinterSettingAsync(request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }
}