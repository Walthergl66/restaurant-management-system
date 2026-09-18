using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Restaurant.Domain.Finance;

namespace Restaurant.Infrastructure.Persistence.Configurations;

public sealed class IncomeConfiguration : IEntityTypeConfiguration<Income>
{
    public void Configure(EntityTypeBuilder<Income> builder)
    {
        builder.ToTable("incomes");

        builder.HasKey(i => i.Id);

        builder.Property(i => i.Amount)
            .HasPrecision(18, 2);

        builder.Property(i => i.Source)
            .HasConversion<string>()
            .HasMaxLength(20)
            .IsRequired();

        builder.Property(i => i.Description)
            .HasMaxLength(300);

        builder.HasIndex(i => i.OccurredAtUtc);
        builder.HasIndex(i => i.ReferenceId);
    }
}

public sealed class ExpenseConfiguration : IEntityTypeConfiguration<Expense>
{
    public void Configure(EntityTypeBuilder<Expense> builder)
    {
        builder.ToTable("expenses");

        builder.HasKey(e => e.Id);

        builder.Property(e => e.Amount)
            .HasPrecision(18, 2);

        builder.Property(e => e.Category)
            .HasConversion<string>()
            .HasMaxLength(30)
            .IsRequired();

        builder.Property(e => e.Description)
            .HasMaxLength(300)
            .IsRequired();

        builder.Property(e => e.Reference)
            .HasMaxLength(200);

        builder.HasIndex(e => e.OccurredAtUtc);
        builder.HasIndex(e => e.Category);
    }
}