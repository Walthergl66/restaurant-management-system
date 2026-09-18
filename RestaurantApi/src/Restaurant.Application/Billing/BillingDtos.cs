using Restaurant.Application.Common;

namespace Restaurant.Application.Billing;

public sealed record BillingCustomerRequest(
    string? Identification,
    string? TaxId,
    string? LegalName,
    string? Address,
    string? Phone,
    string? Email,
    bool IsFinalConsumer);

public sealed record InvoiceItemRequest(
    Guid ProductId,
    string Description,
    int Quantity,
    decimal UnitPrice);

public sealed record CreateInvoiceRequest(
    string InvoiceNumber,
    BillingCustomerRequest Customer,
    Guid? SaleId,
    Guid? TableAccountId,
    decimal Subtotal,
    decimal Discount,
    decimal Tip,
    IReadOnlyCollection<InvoiceItemRequest> Items);

public sealed record BillingCustomerResponse(
    string? Identification,
    string? TaxId,
    string? LegalName,
    string? Address,
    string? Phone,
    string? Email,
    bool IsFinalConsumer);

public sealed record InvoiceItemResponse(
    Guid Id,
    Guid ProductId,
    string Description,
    int Quantity,
    decimal UnitPrice,
    decimal LineTotal);

public sealed record InvoiceResponse(
    Guid Id,
    string InvoiceNumber,
    Guid? SaleId,
    Guid? TableAccountId,
    string Status,
    decimal Subtotal,
    decimal Discount,
    decimal Tip,
    decimal Total,
    string? AuthorizationNumber,
    DateTime? IssuedAtUtc,
    BillingCustomerResponse Customer,
    IReadOnlyCollection<InvoiceItemResponse> Items);

public interface IElectronicInvoiceProvider
{
    Task<string?> IssueAsync(InvoiceResponse invoice, CancellationToken cancellationToken);
}

public interface IBillingService
{
    Task<Result<InvoiceResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<InvoiceResponse>> GetBySaleAsync(Guid saleId, CancellationToken cancellationToken);

    Task<Result<InvoiceResponse>> CreateAsync(CreateInvoiceRequest request, CancellationToken cancellationToken);

    Task<Result<InvoiceResponse>> IssueAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<InvoiceResponse>> CancelAsync(Guid id, CancellationToken cancellationToken);
}