using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Restaurant.Domain.Preparation;

namespace Restaurant.Infrastructure.Persistence.Configurations;

public sealed class PreparationOrderConfiguration : IEntityTypeConfiguration<PreparationOrder>
{
    public void Configure(EntityTypeBuilder<PreparationOrder> builder)
    {
        builder.ToTable("preparation_orders");

        builder.HasKey(o => o.Id);

        builder.Property(o => o.PreparationAreaName)
            .HasMaxLength(100)
            .IsRequired();

        builder.HasIndex(o => o.SourceOrderId);

        builder.HasMany(o => o.Items)
            .WithOne()
            .HasForeignKey(i => i.PreparationOrderId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}

public sealed class PreparationOrderItemConfiguration : IEntityTypeConfiguration<PreparationOrderItem>
{
    public void Configure(EntityTypeBuilder<PreparationOrderItem> builder)
    {
        builder.ToTable("preparation_order_items");

        builder.HasKey(i => i.Id);

        builder.Property(i => i.ProductName)
            .HasMaxLength(150)
            .IsRequired();

        builder.Property(i => i.Notes)
            .HasMaxLength(500);
    }
}