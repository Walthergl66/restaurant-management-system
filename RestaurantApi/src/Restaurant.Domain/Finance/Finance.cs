using Restaurant.Domain.Common;

namespace Restaurant.Domain.Finance;

public enum IncomeSource
{
    SALE,
    PAYMENT,
    MANUAL,
    OTHER,
}

public sealed class Income : AuditableEntity
{
    public decimal Amount { get; private set; }

    public IncomeSource Source { get; private set; }

    public Guid? ReferenceId { get; private set; }

    public string? Description { get; private set; }

    public DateTime OccurredAtUtc { get; private set; } = DateTime.UtcNow;

    public Guid? CreatedByUserId { get; private set; }

    public static Income Create(
        decimal amount,
        IncomeSource source,
        Guid? referenceId = null,
        string? description = null,
        Guid? createdByUserId = null)
    {
        if (amount <= 0)
        {
            throw new DomainException("El monto del ingreso debe ser mayor a cero.");
        }

        return new Income
        {
            Amount = amount,
            Source = source,
            ReferenceId = referenceId,
            Description = description,
            CreatedByUserId = createdByUserId,
        };
    }
}

public enum ExpenseCategory
{
    RAW_MATERIAL,
    SUPPLIERS,
    UTILITIES,
    MAINTENANCE,
    TRANSPORT,
    ADVERTISING,
    CLEANING,
    RENT,
    PAYROLL,
    TAXES,
    OTHER,
}

public sealed class Expense : AuditableEntity
{
    public decimal Amount { get; private set; }

    public ExpenseCategory Category { get; private set; }

    public string Description { get; private set; } = string.Empty;

    public string? Reference { get; private set; }

    public DateTime OccurredAtUtc { get; private set; } = DateTime.UtcNow;

    public Guid? CreatedByUserId { get; private set; }

    public static Expense Create(
        decimal amount,
        ExpenseCategory category,
        string description,
        string? reference = null,
        Guid? createdByUserId = null)
    {
        if (amount <= 0)
        {
            throw new DomainException("El monto del egreso debe ser mayor a cero.");
        }

        if (string.IsNullOrWhiteSpace(description))
        {
            throw new DomainException("La descripción del egreso es obligatoria.");
        }

        return new Expense
        {
            Amount = amount,
            Category = category,
            Description = description.Trim(),
            Reference = reference,
            CreatedByUserId = createdByUserId,
        };
    }
}