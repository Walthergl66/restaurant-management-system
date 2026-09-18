using Restaurant.Application.Common;

namespace Restaurant.Application.Payments;

public sealed record PaymentResponse(
    Guid Id,
    Guid? TableAccountId,
    Guid? SaleId,
    decimal Amount,
    string Method,
    string Status,
    Guid? ProcessedBy,
    DateTime? ProcessedAtUtc,
    string? Reference);

public sealed record CreateAccountPaymentRequest(
    decimal Amount,
    string Method,
    string? Reference);

public sealed record SaleItemRequest(
    Guid ProductId,
    int Quantity);

public sealed record CreateSaleRequest(
    string SaleNumber,
    Guid TableAccountId,
    IReadOnlyCollection<SaleItemRequest> Items);

public sealed record SaleItemResponse(
    Guid Id,
    Guid ProductId,
    string ProductName,
    int Quantity,
    decimal UnitPrice,
    decimal LineTotal);

public sealed record SaleResponse(
    Guid Id,
    Guid TableAccountId,
    string SaleNumber,
    decimal Subtotal,
    decimal Discount,
    decimal Tip,
    decimal Total,
    decimal PaidAmount,
    decimal AmountDue,
    bool IsFullyPaid,
    Guid? InvoiceId,
    IReadOnlyCollection<SaleItemResponse> Items);

public interface IPaymentService
{
    Task<Result<PaymentResponse>> CreateForAccountAsync(
        Guid accountId,
        CreateAccountPaymentRequest request,
        Guid userId,
        CancellationToken cancellationToken);

    Task<Result<PaymentResponse>> CreateForSaleAsync(
        Guid saleId,
        CreateAccountPaymentRequest request,
        Guid userId,
        CancellationToken cancellationToken);
}

public interface ISaleService
{
    Task<Result<SaleResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<SaleResponse>> GetByAccountAsync(Guid accountId, CancellationToken cancellationToken);

    Task<Result<SaleResponse>> CreateAsync(CreateSaleRequest request, CancellationToken cancellationToken);

    Task<Result<SaleResponse>> AddItemAsync(Guid saleId, SaleItemRequest request, CancellationToken cancellationToken);
}