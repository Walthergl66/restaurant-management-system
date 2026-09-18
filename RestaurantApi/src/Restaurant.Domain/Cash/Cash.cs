using Restaurant.Domain.Common;

namespace Restaurant.Domain.Cash;

public enum CashRegisterStatus
{
    OPEN,
    CLOSED,
}

public enum CashMovementType
{
    SALE,
    INCOME,
    EXPENSE,
    WITHDRAWAL,
    ADJUSTMENT,
}

public sealed class CashRegister : AuditableEntity
{
    public string Name { get; private set; } = string.Empty;

    public string? Description { get; private set; }

    public bool IsActive { get; private set; } = true;

    public static CashRegister Create(string name, string? description = null)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new DomainException("El nombre de la caja es obligatorio.");
        }

        return new CashRegister
        {
            Name = name.Trim(),
            Description = description,
        };
    }

    public void Update(string name, string? description, bool isActive)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new DomainException("El nombre de la caja es obligatorio.");
        }

        Name = name.Trim();
        Description = description;
        IsActive = isActive;
    }

    public void Activate() => IsActive = true;

    public void Deactivate() => IsActive = false;
}

public sealed class CashMovement
{
    public Guid Id { get; private set; } = Guid.NewGuid();

    public Guid CashOpeningId { get; private set; }

    public CashMovementType Type { get; private set; }

    public decimal Amount { get; private set; }

    public string? Reference { get; private set; }

    public string? Notes { get; private set; }

    public Guid CreatedByUserId { get; private set; }

    public DateTime CreatedAtUtc { get; private set; } = DateTime.UtcNow;

    public decimal SignedAmount => Type switch
    {
        CashMovementType.EXPENSE => -Amount,
        CashMovementType.WITHDRAWAL => -Amount,
        _ => Amount,
    };

    internal static CashMovement Create(
        Guid cashOpeningId,
        CashMovementType type,
        decimal amount,
        Guid createdByUserId,
        string? reference,
        string? notes)
    {
        if (type == CashMovementType.ADJUSTMENT)
        {
            if (amount == 0)
            {
                throw new DomainException("El ajuste no puede ser cero.");
            }
        }
        else if (amount <= 0)
        {
            throw new DomainException("El monto del movimiento debe ser mayor a cero.");
        }

        return new CashMovement
        {
            CashOpeningId = cashOpeningId,
            Type = type,
            Amount = amount,
            CreatedByUserId = createdByUserId,
            Reference = reference,
            Notes = notes,
        };
    }
}

public sealed class CashOpening : AggregateRoot
{
    public Guid CashRegisterId { get; private set; }

    public Guid OpenedByUserId { get; private set; }

    public DateTime OpenedAtUtc { get; private set; } = DateTime.UtcNow;

    public decimal InitialFund { get; private set; }

    public CashRegisterStatus Status { get; private set; } = CashRegisterStatus.OPEN;

    public Guid? ClosedByUserId { get; private set; }

    public DateTime? ClosedAtUtc { get; private set; }

    public decimal? ExpectedAmount { get; private set; }

    public decimal? CountedAmount { get; private set; }

    public decimal? Difference { get; private set; }

    public IReadOnlyCollection<CashMovement> Movements => _movements.AsReadOnly();

    private readonly List<CashMovement> _movements = [];

    public static CashOpening Open(Guid cashRegisterId, Guid openedByUserId, decimal initialFund)
    {
        if (initialFund < 0)
        {
            throw new DomainException("El fondo inicial no puede ser negativo.");
        }

        return new CashOpening
        {
            CashRegisterId = cashRegisterId,
            OpenedByUserId = openedByUserId,
            InitialFund = initialFund,
        };
    }

    public void RegisterMovement(CashMovementType type, decimal amount, Guid createdByUserId, string? reference, string? notes)
    {
        if (Status != CashRegisterStatus.OPEN)
        {
            throw new DomainException("No se pueden registrar movimientos en una caja cerrada.");
        }

        _movements.Add(CashMovement.Create(Id, type, amount, createdByUserId, reference, notes));
    }

    public decimal TotalIncome => _movements.Where(m => m.SignedAmount > 0).Sum(m => m.SignedAmount);

    public decimal TotalExpense => _movements.Where(m => m.SignedAmount < 0).Sum(m => m.SignedAmount);

    public decimal ComputeExpected() => InitialFund + _movements.Sum(m => m.SignedAmount);

    public void Close(Guid closedByUserId, decimal countedAmount)
    {
        if (Status == CashRegisterStatus.CLOSED)
        {
            throw new DomainException("La caja ya está cerrada.");
        }

        if (countedAmount < 0)
        {
            throw new DomainException("El monto contado no puede ser negativo.");
        }

        ExpectedAmount = ComputeExpected();
        CountedAmount = countedAmount;
        Difference = countedAmount - ExpectedAmount.Value;
        ClosedByUserId = closedByUserId;
        ClosedAtUtc = DateTime.UtcNow;
        Status = CashRegisterStatus.CLOSED;
    }
}