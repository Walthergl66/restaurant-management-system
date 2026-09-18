using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Restaurant.Domain.Cancellations;

namespace Restaurant.Infrastructure.Persistence.Configurations;

public sealed class CancellationRequestConfiguration : IEntityTypeConfiguration<CancellationRequest>
{
    public void Configure(EntityTypeBuilder<CancellationRequest> builder)
    {
        builder.ToTable("cancellation_requests");

        builder.HasKey(r => r.Id);

        builder.Property(r => r.ProductName)
            .HasMaxLength(150)
            .IsRequired();

        builder.Property(r => r.Reason)
            .HasMaxLength(500)
            .IsRequired();

        builder.Property(r => r.ReviewNote)
            .HasMaxLength(500);
    }
}