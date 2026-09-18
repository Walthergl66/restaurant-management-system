using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Restaurant.Domain.Customers;

namespace Restaurant.Infrastructure.Persistence.Configurations;

public sealed class CustomerProfileConfiguration : IEntityTypeConfiguration<CustomerProfile>
{
    public void Configure(EntityTypeBuilder<CustomerProfile> builder)
    {
        builder.ToTable("customer_profiles");

        builder.HasKey(c => c.Id);

        builder.Property(c => c.FullName)
            .HasMaxLength(200)
            .IsRequired();

        builder.Property(c => c.Phone)
            .HasMaxLength(50);

        builder.Property(c => c.Email)
            .HasMaxLength(200);

        builder.HasIndex(c => c.UserId);

        builder.OwnsMany(c => c.Addresses, address =>
        {
            address.ToTable("customer_addresses");
            address.WithOwner().HasForeignKey(a => a.CustomerProfileId);
            address.HasKey(a => a.Id);
            address.Property(a => a.Label).HasMaxLength(100).IsRequired();
            address.Property(a => a.Line1).HasMaxLength(300).IsRequired();
            address.Property(a => a.Line2).HasMaxLength(300);
            address.Property(a => a.City).HasMaxLength(100).IsRequired();
            address.Property(a => a.Reference).HasMaxLength(300);
        });
    }
}