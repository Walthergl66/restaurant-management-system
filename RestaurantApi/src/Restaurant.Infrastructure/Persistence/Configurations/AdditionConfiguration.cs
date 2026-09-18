using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Restaurant.Domain.Additions;

namespace Restaurant.Infrastructure.Persistence.Configurations;

public sealed class AdditionConfiguration : IEntityTypeConfiguration<Addition>
{
    public void Configure(EntityTypeBuilder<Addition> builder)
    {
        builder.ToTable("additions");

        builder.HasKey(a => a.Id);

        builder.Property(a => a.Subtotal)
            .HasPrecision(18, 2)
            .IsRequired();

        builder.Property(a => a.Total)
            .HasPrecision(18, 2)
            .IsRequired();

        builder.HasMany(a => a.Items)
            .WithOne()
            .HasForeignKey(i => i.AdditionId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}

public sealed class AdditionItemConfiguration : IEntityTypeConfiguration<AdditionItem>
{
    public void Configure(EntityTypeBuilder<AdditionItem> builder)
    {
        builder.ToTable("addition_items");

        builder.HasKey(i => i.Id);

        builder.Property(i => i.ProductName)
            .HasMaxLength(150)
            .IsRequired();

        builder.Property(i => i.UnitPrice)
            .HasPrecision(18, 2)
            .IsRequired();

        builder.Property(i => i.LineTotal)
            .HasPrecision(18, 2)
            .IsRequired();

        builder.HasMany(i => i.Extras)
            .WithOne()
            .HasForeignKey(e => e.AdditionId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.HasMany(i => i.RemovedIngredients)
            .WithOne()
            .HasForeignKey(r => r.AdditionId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}

public sealed class AdditionItemExtraConfiguration : IEntityTypeConfiguration<AdditionItemExtra>
{
    public void Configure(EntityTypeBuilder<AdditionItemExtra> builder)
    {
        builder.ToTable("addition_item_extras");

        builder.HasKey(e => e.Id);

        builder.Property(e => e.Name)
            .HasMaxLength(100)
            .IsRequired();

        builder.Property(e => e.Price)
            .HasPrecision(18, 2)
            .IsRequired();
    }
}

public sealed class AdditionItemRemovedIngredientConfiguration : IEntityTypeConfiguration<AdditionItemRemovedIngredient>
{
    public void Configure(EntityTypeBuilder<AdditionItemRemovedIngredient> builder)
    {
        builder.ToTable("addition_item_removed_ingredients");

        builder.HasKey(r => r.Id);

        builder.Property(r => r.IngredientName)
            .HasMaxLength(100)
            .IsRequired();
    }
}