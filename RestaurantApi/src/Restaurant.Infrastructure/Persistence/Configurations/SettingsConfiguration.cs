using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Restaurant.Domain.Settings;

namespace Restaurant.Infrastructure.Persistence.Configurations;

public sealed class RestaurantSettingsConfiguration : IEntityTypeConfiguration<RestaurantSettings>
{
    public void Configure(EntityTypeBuilder<RestaurantSettings> builder)
    {
        builder.ToTable("restaurant_settings");

        builder.HasKey(s => s.Id);

        builder.Property(s => s.RestaurantName)
            .HasMaxLength(200)
            .IsRequired();

        builder.Property(s => s.LegalName)
            .HasMaxLength(200);

        builder.Property(s => s.TaxId)
            .HasMaxLength(50);

        builder.Property(s => s.Address)
            .HasMaxLength(300);

        builder.Property(s => s.Phone)
            .HasMaxLength(50);

        builder.Property(s => s.Email)
            .HasMaxLength(200);

        builder.Property(s => s.Currency)
            .HasMaxLength(10)
            .IsRequired();

        builder.Property(s => s.TimeZone)
            .HasMaxLength(100)
            .IsRequired();

        builder.Property(s => s.DefaultTaxRate)
            .HasPrecision(5, 2);

        builder.OwnsOne(s => s.Payment, payment =>
        {
            payment.Property(p => p.TipSuggestionPercent)
                .HasPrecision(5, 2);

            payment.Property(p => p.AcceptedMethods)
                .HasColumnType("text[]");
        });

        builder.OwnsOne(s => s.Cancellation, cancellation =>
        {
        });
    }
}

public sealed class PrinterSettingConfiguration : IEntityTypeConfiguration<PrinterSetting>
{
    public void Configure(EntityTypeBuilder<PrinterSetting> builder)
    {
        builder.ToTable("printer_settings");

        builder.HasKey(p => p.Id);

        builder.HasIndex(p => p.PreparationAreaId)
            .IsUnique();
    }
}