using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Restaurant.Domain.Printing;

namespace Restaurant.Infrastructure.Persistence.Configurations;

public sealed class PrinterConfiguration : IEntityTypeConfiguration<Printer>
{
    public void Configure(EntityTypeBuilder<Printer> builder)
    {
        builder.ToTable("printers");

        builder.HasKey(p => p.Id);

        builder.Property(p => p.Name)
            .HasMaxLength(100)
            .IsRequired();

        builder.Property(p => p.Description)
            .HasMaxLength(300);

        builder.Property(p => p.IpAddress)
            .HasMaxLength(50);

        builder.Property(p => p.Port)
            .HasMaxLength(20);

        builder.Property(p => p.PreparationAreaIds)
            .HasColumnType("uuid[]");

        builder.HasIndex(p => p.Name)
            .IsUnique();
    }
}

public sealed class PrintJobConfiguration : IEntityTypeConfiguration<PrintJob>
{
    public void Configure(EntityTypeBuilder<PrintJob> builder)
    {
        builder.ToTable("print_jobs");

        builder.HasKey(p => p.Id);

        builder.Property(p => p.IdempotencyKey)
            .HasMaxLength(200)
            .IsRequired();

        builder.Property(p => p.Error)
            .HasMaxLength(1000);

        builder.HasIndex(p => p.IdempotencyKey)
            .IsUnique();
    }
}