using Microsoft.EntityFrameworkCore;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Settings;
using Restaurant.Infrastructure.Persistence;

namespace Restaurant.Infrastructure.Repositories;

public sealed class SettingsRepository(ApplicationDbContext dbContext) : ISettingsRepository
{
    public async Task<RestaurantSettings?> GetAsync(CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<RestaurantSettings>()
            .FirstOrDefaultAsync(cancellationToken);
    }

    public async Task AddAsync(RestaurantSettings settings, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<RestaurantSettings>().AddAsync(settings, cancellationToken);
    }

    public async Task<IReadOnlyCollection<PrinterSetting>> GetPrinterSettingsAsync(CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<PrinterSetting>()
            .AsNoTracking()
            .OrderBy(p => p.PreparationAreaId)
            .ToListAsync(cancellationToken);
    }

    public async Task<PrinterSetting?> GetPrinterSettingByAreaAsync(Guid preparationAreaId, CancellationToken cancellationToken = default)
    {
        return await dbContext.Set<PrinterSetting>()
            .FirstOrDefaultAsync(p => p.PreparationAreaId == preparationAreaId, cancellationToken);
    }

    public async Task AddPrinterSettingAsync(PrinterSetting printerSetting, CancellationToken cancellationToken = default)
    {
        await dbContext.Set<PrinterSetting>().AddAsync(printerSetting, cancellationToken);
    }

    public Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        return dbContext.SaveChangesAsync(cancellationToken);
    }
}