using Restaurant.Domain.Common;

namespace Restaurant.Domain.Settings;

public sealed class PaymentSettings : ValueObject
{
    public bool AllowSplitPayment { get; private set; } = true;

    public bool RequirePaidBeforeInvoice { get; private set; } = true;

    public decimal TipSuggestionPercent { get; private set; } = 10m;

    public List<string> AcceptedMethods { get; private set; } = ["CASH", "CARD", "TRANSFER", "OTHER"];

    private PaymentSettings()
    {
    }

    private PaymentSettings(bool allowSplitPayment, bool requirePaidBeforeInvoice, decimal tipSuggestionPercent, List<string> acceptedMethods)
    {
        AllowSplitPayment = allowSplitPayment;
        RequirePaidBeforeInvoice = requirePaidBeforeInvoice;
        TipSuggestionPercent = tipSuggestionPercent;
        AcceptedMethods = acceptedMethods;
    }

    public static PaymentSettings Create(
        bool allowSplitPayment,
        bool requirePaidBeforeInvoice,
        decimal tipSuggestionPercent,
        IEnumerable<string>? acceptedMethods = null)
    {
        if (tipSuggestionPercent < 0 || tipSuggestionPercent > 100)
        {
            throw new DomainException("El porcentaje de propina debe estar entre 0 y 100.");
        }

        var methods = (acceptedMethods ?? ["CASH", "CARD", "TRANSFER", "OTHER"])
            .Select(m => m.Trim().ToUpperInvariant())
            .Where(m => m.Length > 0)
            .Distinct()
            .ToList();

        if (methods.Count == 0)
        {
            throw new DomainException("Debe existir al menos un método de pago aceptado.");
        }

        return new PaymentSettings(allowSplitPayment, requirePaidBeforeInvoice, tipSuggestionPercent, methods);
    }

    protected override IEnumerable<object?> GetEqualityComponents()
    {
        yield return AllowSplitPayment;
        yield return RequirePaidBeforeInvoice;
        yield return TipSuggestionPercent;
        foreach (var method in AcceptedMethods)
        {
            yield return method;
        }
    }
}

public sealed class CancellationSettings : ValueObject
{
    public bool RequireSupervisorApproval { get; private set; } = true;

    public bool AllowAfterConfirmation { get; private set; } = true;

    public int? MaxMinutesAfterConfirmation { get; private set; }

    public bool RequireReason { get; private set; } = true;

    private CancellationSettings()
    {
    }

    private CancellationSettings(bool requireSupervisorApproval, bool allowAfterConfirmation, int? maxMinutesAfterConfirmation, bool requireReason)
    {
        RequireSupervisorApproval = requireSupervisorApproval;
        AllowAfterConfirmation = allowAfterConfirmation;
        MaxMinutesAfterConfirmation = maxMinutesAfterConfirmation;
        RequireReason = requireReason;
    }

    public static CancellationSettings Create(
        bool requireSupervisorApproval,
        bool allowAfterConfirmation,
        int? maxMinutesAfterConfirmation,
        bool requireReason)
    {
        if (maxMinutesAfterConfirmation is < 0)
        {
            throw new DomainException("El tiempo máximo para anular no puede ser negativo.");
        }

        return new CancellationSettings(
            requireSupervisorApproval,
            allowAfterConfirmation,
            maxMinutesAfterConfirmation,
            requireReason);
    }

    protected override IEnumerable<object?> GetEqualityComponents()
    {
        yield return RequireSupervisorApproval;
        yield return AllowAfterConfirmation;
        yield return MaxMinutesAfterConfirmation;
        yield return RequireReason;
    }
}

public sealed class RestaurantSettings : AuditableEntity
{
    public string RestaurantName { get; private set; } = string.Empty;

    public string? LegalName { get; private set; }

    public string? TaxId { get; private set; }

    public string? Address { get; private set; }

    public string? Phone { get; private set; }

    public string? Email { get; private set; }

    public string Currency { get; private set; } = "USD";

    public string TimeZone { get; private set; } = "America/Guayaquil";

    public bool AllowDineIn { get; private set; } = true;

    public bool AllowPickup { get; private set; } = true;

    public bool AllowDelivery { get; private set; } = true;

    public decimal DefaultTaxRate { get; private set; }

    public PaymentSettings Payment { get; private set; } = null!;

    public CancellationSettings Cancellation { get; private set; } = null!;

    public static RestaurantSettings CreateDefault()
    {
        return new RestaurantSettings
        {
            RestaurantName = "Restaurante",
            Payment = PaymentSettings.Create(true, true, 10m),
            Cancellation = CancellationSettings.Create(true, true, null, true),
        };
    }

    public void Update(
        string restaurantName,
        string? legalName,
        string? taxId,
        string? address,
        string? phone,
        string? email,
        string currency,
        string timeZone,
        bool allowDineIn,
        bool allowPickup,
        bool allowDelivery,
        decimal defaultTaxRate)
    {
        if (string.IsNullOrWhiteSpace(restaurantName))
        {
            throw new DomainException("El nombre del restaurante es obligatorio.");
        }

        if (defaultTaxRate < 0 || defaultTaxRate > 100)
        {
            throw new DomainException("La tasa de impuesto debe estar entre 0 y 100.");
        }

        RestaurantName = restaurantName.Trim();
        LegalName = legalName?.Trim();
        TaxId = taxId?.Trim();
        Address = address?.Trim();
        Phone = phone?.Trim();
        Email = email?.Trim();
        Currency = string.IsNullOrWhiteSpace(currency) ? "USD" : currency.Trim().ToUpperInvariant();
        TimeZone = string.IsNullOrWhiteSpace(timeZone) ? "UTC" : timeZone.Trim();
        AllowDineIn = allowDineIn;
        AllowPickup = allowPickup;
        AllowDelivery = allowDelivery;
        DefaultTaxRate = defaultTaxRate;
    }

    public void UpdatePayment(PaymentSettings payment) => Payment = payment;

    public void UpdateCancellation(CancellationSettings cancellation) => Cancellation = cancellation;
}

public sealed class PrinterSetting : AuditableEntity
{
    public Guid PreparationAreaId { get; private set; }

    public Guid PrinterId { get; private set; }

    public int Copies { get; private set; } = 1;

    public bool IsActive { get; private set; } = true;

    public static PrinterSetting Create(Guid preparationAreaId, Guid printerId, int copies = 1)
    {
        if (preparationAreaId == Guid.Empty)
        {
            throw new DomainException("El área de preparación es obligatoria.");
        }

        if (printerId == Guid.Empty)
        {
            throw new DomainException("La impresora es obligatoria.");
        }

        if (copies <= 0)
        {
            throw new DomainException("El número de copias debe ser mayor a cero.");
        }

        return new PrinterSetting
        {
            PreparationAreaId = preparationAreaId,
            PrinterId = printerId,
            Copies = copies,
        };
    }

    public void Update(Guid printerId, int copies, bool isActive)
    {
        if (printerId == Guid.Empty)
        {
            throw new DomainException("La impresora es obligatoria.");
        }

        if (copies <= 0)
        {
            throw new DomainException("El número de copias debe ser mayor a cero.");
        }

        PrinterId = printerId;
        Copies = copies;
        IsActive = isActive;
    }
}