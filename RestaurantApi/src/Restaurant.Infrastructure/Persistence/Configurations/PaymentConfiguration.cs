using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Restaurant.Domain.Payments;
using Restaurant.Domain.Sales;

namespace Restaurant.Infrastructure.Persistence.Configurations;

public sealed class PaymentConfiguration : IEntityTypeConfiguration<Payment>
{
    public void Configure(EntityTypeBuilder<Payment> builder)
    {
        builder.ToTable("payments");

        builder.HasKey(p => p.Id);

        builder.Property(p => p.Amount)
            .HasPrecision(18, 2);

        builder.Property(p => p.Method)
            .HasConversion<string>()
            .HasMaxLength(20)
            .IsRequired();

        builder.Property(p => p.Status)
            .HasConversion<string>()
            .HasMaxLength(20)
            .IsRequired();

        builder.Property(p => p.Reference)
            .HasMaxLength(200);

        builder.HasIndex(p => p.TableAccountId);
        builder.HasIndex(p => p.SaleId);
    }
}

public sealed class SaleConfiguration : IEntityTypeConfiguration<Sale>
{
    public void Configure(EntityTypeBuilder<Sale> builder)
    {
        builder.ToTable("sales");

        builder.HasKey(s => s.Id);

        builder.Property(s => s.SaleNumber)
            .HasMaxLength(50)
            .IsRequired();

        builder.Property(s => s.Subtotal)
            .HasPrecision(18, 2);

        builder.Property(s => s.Discount)
            .HasPrecision(18, 2);

        builder.Property(s => s.Tip)
            .HasPrecision(18, 2);

        builder.Property(s => s.PaidAmount)
            .HasPrecision(18, 2);

        builder.HasIndex(s => s.SaleNumber)
            .IsUnique();

        builder.HasIndex(s => s.TableAccountId);

        builder.OwnsMany(s => s.Items, item =>
        {
            item.ToTable("sale_items");
            item.WithOwner().HasForeignKey(i => i.SaleId);
            item.HasKey(i => i.Id);
            item.Property(i => i.ProductName).HasMaxLength(200).IsRequired();
            item.Property(i => i.UnitPrice).HasPrecision(18, 2);
        });
    }
}