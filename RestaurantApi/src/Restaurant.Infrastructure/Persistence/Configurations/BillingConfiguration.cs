using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Restaurant.Domain.Billing;

namespace Restaurant.Infrastructure.Persistence.Configurations;

public sealed class InvoiceConfiguration : IEntityTypeConfiguration<Invoice>
{
    public void Configure(EntityTypeBuilder<Invoice> builder)
    {
        builder.ToTable("invoices");

        builder.HasKey(i => i.Id);

        builder.Property(i => i.InvoiceNumber)
            .HasMaxLength(50)
            .IsRequired();

        builder.Property(i => i.Status)
            .HasConversion<string>()
            .HasMaxLength(20)
            .IsRequired();

        builder.Property(i => i.Subtotal)
            .HasPrecision(18, 2);

        builder.Property(i => i.Discount)
            .HasPrecision(18, 2);

        builder.Property(i => i.Tip)
            .HasPrecision(18, 2);

        builder.Property(i => i.AuthorizationNumber)
            .HasMaxLength(100);

        builder.HasIndex(i => i.InvoiceNumber)
            .IsUnique();

        builder.HasIndex(i => i.SaleId);

        builder.OwnsOne(i => i.CustomerData, customer =>
        {
            customer.Property(c => c.Identification).HasMaxLength(50);
            customer.Property(c => c.TaxId).HasMaxLength(50);
            customer.Property(c => c.LegalName).HasMaxLength(200);
            customer.Property(c => c.Address).HasMaxLength(300);
            customer.Property(c => c.Phone).HasMaxLength(50);
            customer.Property(c => c.Email).HasMaxLength(200);
        });

        builder.OwnsMany(i => i.Items, item =>
        {
            item.ToTable("invoice_items");
            item.WithOwner().HasForeignKey(i => i.InvoiceId);
            item.HasKey(i => i.Id);
            item.Property(i => i.Description).HasMaxLength(300).IsRequired();
            item.Property(i => i.UnitPrice).HasPrecision(18, 2);
        });
    }
}