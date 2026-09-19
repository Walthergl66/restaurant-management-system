using Restaurant.Domain.Settings;

namespace Restaurant.Application.Common.Abstractions;

public interface ISettingsRepository
{
    Task<RestaurantSettings?> GetAsync(CancellationToken cancellationToken = default);

    Task AddAsync(RestaurantSettings settings, CancellationToken cancellationToken = default);

    Task<IReadOnlyCollection<PrinterSetting>> GetPrinterSettingsAsync(CancellationToken cancellationToken = default);

    Task<PrinterSetting?> GetPrinterSettingByAreaAsync(Guid preparationAreaId, CancellationToken cancellationToken = default);

    Task AddPrinterSettingAsync(PrinterSetting printerSetting, CancellationToken cancellationToken = default);

    Task SaveChangesAsync(CancellationToken cancellationToken = default);
}