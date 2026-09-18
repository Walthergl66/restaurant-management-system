using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Restaurant.Domain.Catalog;

namespace Restaurant.Infrastructure.Persistence.Configurations;

public sealed class CategoryConfiguration : IEntityTypeConfiguration<Category>
{
    public void Configure(EntityTypeBuilder<Category> builder)
    {
        builder.ToTable("categories");
        builder.HasKey(c => c.Id);
        builder.Property(c => c.Name).HasMaxLength(100).IsRequired();
        builder.Property(c => c.Description).HasMaxLength(500);
    }
}

public sealed class PreparationAreaConfiguration : IEntityTypeConfiguration<PreparationArea>
{
    public void Configure(EntityTypeBuilder<PreparationArea> builder)
    {
        builder.ToTable("preparation_areas");
        builder.HasKey(a => a.Id);
        builder.Property(a => a.Name).HasMaxLength(100).IsRequired();
        builder.Property(a => a.Description).HasMaxLength(500);
    }
}

public sealed class ExtraConfiguration : IEntityTypeConfiguration<Extra>
{
    public void Configure(EntityTypeBuilder<Extra> builder)
    {
        builder.ToTable("extras");
        builder.HasKey(e => e.Id);
        builder.Property(e => e.Name).HasMaxLength(100).IsRequired();
        builder.Property(e => e.Price).HasPrecision(18, 2).IsRequired();
    }
}

public sealed class ProductConfiguration : IEntityTypeConfiguration<Product>
{
    public void Configure(EntityTypeBuilder<Product> builder)
    {
        builder.ToTable("products");
        builder.HasKey(p => p.Id);
        builder.Property(p => p.Name).HasMaxLength(150).IsRequired();
        builder.Property(p => p.Description).HasMaxLength(500);

        builder.HasOne(p => p.Category)
            .WithMany(c => c.Products)
            .HasForeignKey(p => p.CategoryId)
            .OnDelete(DeleteBehavior.Restrict);

        builder.HasOne(p => p.PreparationArea)
            .WithMany()
            .HasForeignKey(p => p.PreparationAreaId)
            .OnDelete(DeleteBehavior.Restrict);
    }
}

public sealed class ProductPriceConfiguration : IEntityTypeConfiguration<ProductPrice>
{
    public void Configure(EntityTypeBuilder<ProductPrice> builder)
    {
        builder.ToTable("product_prices");
        builder.HasKey(p => p.Id);
        builder.Property(p => p.Price).HasPrecision(18, 2).IsRequired();

        builder.HasOne(p => p.Product)
            .WithMany(p => p.Prices)
            .HasForeignKey(p => p.ProductId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.HasIndex(p => new { p.ProductId, p.EffectiveFrom }).IsUnique();
    }
}

public sealed class ProductExtraConfiguration : IEntityTypeConfiguration<ProductExtra>
{
    public void Configure(EntityTypeBuilder<ProductExtra> builder)
    {
        builder.ToTable("product_extras");
        builder.HasKey(p => p.Id);

        builder.HasOne(p => p.Product)
            .WithMany(p => p.AvailableExtras)
            .HasForeignKey(p => p.ProductId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.HasOne(p => p.Extra)
            .WithMany()
            .HasForeignKey(p => p.ExtraId)
            .OnDelete(DeleteBehavior.Restrict);

        builder.HasIndex(p => new { p.ProductId, p.ExtraId }).IsUnique();
    }
}

public sealed class ProductIngredientConfiguration : IEntityTypeConfiguration<ProductIngredient>
{
    public void Configure(EntityTypeBuilder<ProductIngredient> builder)
    {
        builder.ToTable("product_ingredients");
        builder.HasKey(p => p.Id);
        builder.Property(p => p.IngredientName).HasMaxLength(100).IsRequired();

        builder.HasOne(p => p.Product)
            .WithMany(p => p.Ingredients)
            .HasForeignKey(p => p.ProductId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.HasIndex(p => new { p.ProductId, p.IngredientName }).IsUnique();
    }
}