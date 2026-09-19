using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Application.Settings;
using Restaurant.Domain.Common;
using Restaurant.Domain.Settings;

namespace Restaurant.Application.Services;

public sealed class SettingsService(ISettingsRepository settingsRepository) : ISettingsService
{
    public async Task<Result<RestaurantSettingsResponse>> GetAsync(CancellationToken cancellationToken)
    {
        var settings = await GetOrCreateAsync(cancellationToken);

        return ToResponse(settings);
    }

    public async Task<Result<RestaurantSettingsResponse>> UpdateAsync(UpdateRestaurantSettingsRequest request, CancellationToken cancellationToken)
    {
        var settings = await GetOrCreateAsync(cancellationToken);

        try
        {
            settings.Update(
                request.RestaurantName,
                request.LegalName,
                request.TaxId,
                request.Address,
                request.Phone,
                request.Email,
                request.Currency,
                request.TimeZone,
                request.AllowDineIn,
                request.AllowPickup,
                request.AllowDelivery,
                request.DefaultTaxRate);

            await settingsRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(settings);
        }
        catch (DomainException exception)
        {
            return Result<RestaurantSettingsResponse>.ValidationFailure("settings.invalid", exception.Message);
        }
    }

    public async Task<Result<RestaurantSettingsResponse>> UpdatePaymentAsync(UpdatePaymentSettingsRequest request, CancellationToken cancellationToken)
    {
        var settings = await GetOrCreateAsync(cancellationToken);

        try
        {
            var payment = PaymentSettings.Create(
                request.AllowSplitPayment,
                request.RequirePaidBeforeInvoice,
                request.TipSuggestionPercent,
                request.AcceptedMethods);

            settings.UpdatePayment(payment);
            await settingsRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(settings);
        }
        catch (DomainException exception)
        {
            return Result<RestaurantSettingsResponse>.ValidationFailure("settings.invalid", exception.Message);
        }
    }

    public async Task<Result<RestaurantSettingsResponse>> UpdateCancellationAsync(UpdateCancellationSettingsRequest request, CancellationToken cancellationToken)
    {
        var settings = await GetOrCreateAsync(cancellationToken);

        try
        {
            var cancellation = CancellationSettings.Create(
                request.RequireSupervisorApproval,
                request.AllowAfterConfirmation,
                request.MaxMinutesAfterConfirmation,
                request.RequireReason);

            settings.UpdateCancellation(cancellation);
            await settingsRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(settings);
        }
        catch (DomainException exception)
        {
            return Result<RestaurantSettingsResponse>.ValidationFailure("settings.invalid", exception.Message);
        }
    }

    public async Task<Result<IReadOnlyCollection<PrinterSettingResponse>>> GetPrinterSettingsAsync(CancellationToken cancellationToken)
    {
        var printerSettings = await settingsRepository.GetPrinterSettingsAsync(cancellationToken);

        return printerSettings.Select(ToPrinterResponse).ToList();
    }

    public async Task<Result<PrinterSettingResponse>> UpsertPrinterSettingAsync(UpsertPrinterSettingRequest request, CancellationToken cancellationToken)
    {
        try
        {
            var existing = await settingsRepository.GetPrinterSettingByAreaAsync(request.PreparationAreaId, cancellationToken);
            if (existing is null)
            {
                var created = PrinterSetting.Create(request.PreparationAreaId, request.PrinterId, request.Copies);
                await settingsRepository.AddPrinterSettingAsync(created, cancellationToken);
                await settingsRepository.SaveChangesAsync(cancellationToken);

                return ToPrinterResponse(created);
            }

            existing.Update(request.PrinterId, request.Copies, isActive: true);
            await settingsRepository.SaveChangesAsync(cancellationToken);

            return ToPrinterResponse(existing);
        }
        catch (DomainException exception)
        {
            return Result<PrinterSettingResponse>.ValidationFailure("printer_settings.invalid", exception.Message);
        }
    }

    private async Task<RestaurantSettings> GetOrCreateAsync(CancellationToken cancellationToken)
    {
        var settings = await settingsRepository.GetAsync(cancellationToken);
        if (settings is not null)
        {
            return settings;
        }

        settings = RestaurantSettings.CreateDefault();
        await settingsRepository.AddAsync(settings, cancellationToken);
        await settingsRepository.SaveChangesAsync(cancellationToken);

        return settings;
    }

    private static RestaurantSettingsResponse ToResponse(RestaurantSettings settings) => new(
        settings.Id,
        settings.RestaurantName,
        settings.LegalName,
        settings.TaxId,
        settings.Address,
        settings.Phone,
        settings.Email,
        settings.Currency,
        settings.TimeZone,
        settings.AllowDineIn,
        settings.AllowPickup,
        settings.AllowDelivery,
        settings.DefaultTaxRate,
        new PaymentSettingsResponse(
            settings.Payment.AllowSplitPayment,
            settings.Payment.RequirePaidBeforeInvoice,
            settings.Payment.TipSuggestionPercent,
            settings.Payment.AcceptedMethods),
        new CancellationSettingsResponse(
            settings.Cancellation.RequireSupervisorApproval,
            settings.Cancellation.AllowAfterConfirmation,
            settings.Cancellation.MaxMinutesAfterConfirmation,
            settings.Cancellation.RequireReason));

    private static PrinterSettingResponse ToPrinterResponse(PrinterSetting printerSetting) => new(
        printerSetting.Id,
        printerSetting.PreparationAreaId,
        printerSetting.PrinterId,
        printerSetting.Copies,
        printerSetting.IsActive);
}