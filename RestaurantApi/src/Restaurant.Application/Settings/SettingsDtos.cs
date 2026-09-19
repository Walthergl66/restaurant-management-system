using Restaurant.Application.Common;

namespace Restaurant.Application.Settings;

public sealed record PaymentSettingsResponse(
    bool AllowSplitPayment,
    bool RequirePaidBeforeInvoice,
    decimal TipSuggestionPercent,
    IReadOnlyCollection<string> AcceptedMethods);

public sealed record CancellationSettingsResponse(
    bool RequireSupervisorApproval,
    bool AllowAfterConfirmation,
    int? MaxMinutesAfterConfirmation,
    bool RequireReason);

public sealed record RestaurantSettingsResponse(
    Guid Id,
    string RestaurantName,
    string? LegalName,
    string? TaxId,
    string? Address,
    string? Phone,
    string? Email,
    string Currency,
    string TimeZone,
    bool AllowDineIn,
    bool AllowPickup,
    bool AllowDelivery,
    decimal DefaultTaxRate,
    PaymentSettingsResponse Payment,
    CancellationSettingsResponse Cancellation);

public sealed record UpdateRestaurantSettingsRequest(
    string RestaurantName,
    string? LegalName,
    string? TaxId,
    string? Address,
    string? Phone,
    string? Email,
    string Currency,
    string TimeZone,
    bool AllowDineIn,
    bool AllowPickup,
    bool AllowDelivery,
    decimal DefaultTaxRate);

public sealed record UpdatePaymentSettingsRequest(
    bool AllowSplitPayment,
    bool RequirePaidBeforeInvoice,
    decimal TipSuggestionPercent,
    IReadOnlyCollection<string>? AcceptedMethods);

public sealed record UpdateCancellationSettingsRequest(
    bool RequireSupervisorApproval,
    bool AllowAfterConfirmation,
    int? MaxMinutesAfterConfirmation,
    bool RequireReason);

public sealed record PrinterSettingResponse(
    Guid Id,
    Guid PreparationAreaId,
    Guid PrinterId,
    int Copies,
    bool IsActive);

public sealed record UpsertPrinterSettingRequest(Guid PreparationAreaId, Guid PrinterId, int Copies);

public interface ISettingsService
{
    Task<Result<RestaurantSettingsResponse>> GetAsync(CancellationToken cancellationToken);

    Task<Result<RestaurantSettingsResponse>> UpdateAsync(UpdateRestaurantSettingsRequest request, CancellationToken cancellationToken);

    Task<Result<RestaurantSettingsResponse>> UpdatePaymentAsync(UpdatePaymentSettingsRequest request, CancellationToken cancellationToken);

    Task<Result<RestaurantSettingsResponse>> UpdateCancellationAsync(UpdateCancellationSettingsRequest request, CancellationToken cancellationToken);

    Task<Result<IReadOnlyCollection<PrinterSettingResponse>>> GetPrinterSettingsAsync(CancellationToken cancellationToken);

    Task<Result<PrinterSettingResponse>> UpsertPrinterSettingAsync(UpsertPrinterSettingRequest request, CancellationToken cancellationToken);
}