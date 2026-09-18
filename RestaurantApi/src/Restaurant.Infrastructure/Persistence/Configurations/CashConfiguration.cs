using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Restaurant.Domain.Cash;

namespace Restaurant.Infrastructure.Persistence.Configurations;

public sealed class CashRegisterConfiguration : IEntityTypeConfiguration<CashRegister>
{
    public void Configure(EntityTypeBuilder<CashRegister> builder)
    {
        builder.ToTable("cash_registers");

        builder.HasKey(r => r.Id);

        builder.Property(r => r.Name)
            .HasMaxLength(100)
            .IsRequired();

        builder.Property(r => r.Description)
            .HasMaxLength(300);

        builder.HasIndex(r => r.Name)
            .IsUnique();
    }
}

public sealed class CashOpeningConfiguration : IEntityTypeConfiguration<CashOpening>
{
    public void Configure(EntityTypeBuilder<CashOpening> builder)
    {
        builder.ToTable("cash_openings");

        builder.HasKey(o => o.Id);

        builder.Property(o => o.InitialFund)
            .HasPrecision(18, 2);

        builder.Property(o => o.ExpectedAmount)
            .HasPrecision(18, 2);

        builder.Property(o => o.CountedAmount)
            .HasPrecision(18, 2);

        builder.Property(o => o.Difference)
            .HasPrecision(18, 2);

        builder.Property(o => o.Status)
            .HasConversion<string>()
            .HasMaxLength(20)
            .IsRequired();

        builder.HasIndex(o => o.CashRegisterId);

        builder.OwnsMany(o => o.Movements, movement =>
        {
            movement.ToTable("cash_movements");
            movement.WithOwner().HasForeignKey(m => m.CashOpeningId);
            movement.HasKey(m => m.Id);
            movement.Property(m => m.Type).HasConversion<string>().HasMaxLength(20).IsRequired();
            movement.Property(m => m.Amount).HasPrecision(18, 2);
            movement.Property(m => m.Reference).HasMaxLength(200);
            movement.Property(m => m.Notes).HasMaxLength(500);
        });
    }
}