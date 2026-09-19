using Microsoft.EntityFrameworkCore;
using Restaurant.Domain.Audit;

namespace Restaurant.Infrastructure.Persistence.Configurations;

public sealed class AuditConfiguration : IEntityTypeConfiguration<AuditLog>
{
    public void Configure(EntityTypeBuilder<AuditLog> builder)
    {
        builder.ToTable("audit_logs");

        builder.HasKey(l => l.Id);

        builder.Property(l => l.Action)
            .HasMaxLength(100)
            .IsRequired();

        builder.Property(l => l.EntityType)
            .HasMaxLength(100)
            .IsRequired();

        builder.Property(l => l.OldValues)
            .HasColumnType("jsonb");

        builder.Property(l => l.NewValues)
            .HasColumnType("jsonb");

        builder.Property(l => l.IpAddress)
            .HasMaxLength(50);

        builder.HasIndex(l => l.TimestampUtc);
        builder.HasIndex(l => l.UserId);
        builder.HasIndex(l => new { l.EntityType, l.EntityId });
    }
}
