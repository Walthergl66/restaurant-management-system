using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Restaurant.Domain.Tables;

namespace Restaurant.Infrastructure.Persistence.Configurations;

public sealed class RestaurantTableConfiguration : IEntityTypeConfiguration<RestaurantTable>
{
    public void Configure(EntityTypeBuilder<RestaurantTable> builder)
    {
        builder.ToTable("restaurant_tables");

        builder.HasKey(t => t.Id);

        builder.Property(t => t.Name)
            .HasMaxLength(50)
            .IsRequired();

        builder.Property(t => t.Location)
            .HasMaxLength(100);

        builder.HasIndex(t => t.Name)
            .IsUnique();
    }
}

public sealed class TableAccountConfiguration : IEntityTypeConfiguration<TableAccount>
{
    public void Configure(EntityTypeBuilder<TableAccount> builder)
    {
        builder.ToTable("table_accounts");

        builder.HasKey(a => a.Id);

        builder.Property(a => a.AccountNumber)
            .HasMaxLength(50)
            .IsRequired();

        builder.Property(a => a.Subtotal)
            .HasPrecision(18, 2)
            .IsRequired();

        builder.Property(a => a.Discount)
            .HasPrecision(18, 2)
            .IsRequired();

        builder.Property(a => a.Tip)
            .HasPrecision(18, 2)
            .IsRequired();

        builder.Property(a => a.PaidAmount)
            .HasPrecision(18, 2)
            .IsRequired();

        builder.HasIndex(a => a.AccountNumber)
            .IsUnique();
    }
}