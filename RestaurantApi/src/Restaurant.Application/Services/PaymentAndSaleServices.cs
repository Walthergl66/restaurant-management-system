using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Application.Finance;
using Restaurant.Application.Payments;
using Restaurant.Domain.Common;
using Restaurant.Domain.Payments;
using Restaurant.Domain.Sales;

namespace Restaurant.Application.Services;

public sealed class PaymentService(
    IPaymentRepository paymentRepository,
    ITableAccountRepository accountRepository,
    ISaleRepository saleRepository,
    IFinanceService financeService) : IPaymentService
{
    public async Task<Result<PaymentResponse>> CreateForAccountAsync(
        Guid accountId,
        CreateAccountPaymentRequest request,
        Guid userId,
        CancellationToken cancellationToken)
    {
        var account = await accountRepository.GetByIdAsync(accountId, cancellationToken);
        if (account is null)
        {
            return Result<PaymentResponse>.NotFound("account.not_found", "La cuenta no existe.");
        }

        if (!Enum.TryParse<PaymentMethod>(request.Method, true, out var method))
        {
            return Result<PaymentResponse>.ValidationFailure("payment.invalid_method", "El método de pago no es válido.");
        }

        try
        {
            account.RegisterPayment(request.Amount);

            var payment = Payment.Create(request.Amount, method, tableAccountId: accountId, reference: request.Reference);
            payment.MarkPaid(userId);
            await paymentRepository.AddAsync(payment, cancellationToken);
            await paymentRepository.SaveChangesAsync(cancellationToken);

            await financeService.RecordIncomeAsync(
                request.Amount,
                "PAYMENT",
                accountId,
                $"Pago de cuenta {account.AccountNumber}",
                userId,
                cancellationToken);

            return ToPaymentResponse(payment);
        }
        catch (DomainException exception)
        {
            return Result<PaymentResponse>.BusinessRuleFailure("payment.invalid", exception.Message);
        }
    }

    public async Task<Result<PaymentResponse>> CreateForSaleAsync(
        Guid saleId,
        CreateAccountPaymentRequest request,
        Guid userId,
        CancellationToken cancellationToken)
    {
        var sale = await saleRepository.GetByIdAsync(saleId, cancellationToken);
        if (sale is null)
        {
            return Result<PaymentResponse>.NotFound("sale.not_found", "La venta no existe.");
        }

        if (!Enum.TryParse<PaymentMethod>(request.Method, true, out var method))
        {
            return Result<PaymentResponse>.ValidationFailure("payment.invalid_method", "El método de pago no es válido.");
        }

        try
        {
            sale.RegisterPayment(request.Amount);
            await saleRepository.SaveChangesAsync(cancellationToken);

            var payment = Payment.Create(request.Amount, method, saleId: saleId, reference: request.Reference);
            payment.MarkPaid(userId);
            await paymentRepository.AddAsync(payment, cancellationToken);
            await paymentRepository.SaveChangesAsync(cancellationToken);

            await financeService.RecordIncomeAsync(
                request.Amount,
                "SALE",
                saleId,
                $"Pago de venta {sale.SaleNumber}",
                userId,
                cancellationToken);

            return ToPaymentResponse(payment);
        }
        catch (DomainException exception)
        {
            return Result<PaymentResponse>.BusinessRuleFailure("payment.invalid", exception.Message);
        }
    }

    private static PaymentResponse ToPaymentResponse(Payment payment) => new(
        payment.Id,
        payment.TableAccountId,
        payment.SaleId,
        payment.Amount,
        payment.Method.ToString(),
        payment.Status.ToString(),
        payment.ProcessedBy,
        payment.ProcessedAtUtc,
        payment.Reference);
}

public sealed class SaleService(
    ISaleRepository saleRepository,
    IProductRepository productRepository,
    ITableAccountRepository accountRepository) : ISaleService
{
    public async Task<Result<SaleResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var sale = await saleRepository.GetWithItemsAsync(id, cancellationToken);

        return sale is null
            ? Result<SaleResponse>.NotFound("sale.not_found", "La venta no existe.")
            : ToSaleResponse(sale);
    }

    public async Task<Result<SaleResponse>> GetByAccountAsync(Guid accountId, CancellationToken cancellationToken)
    {
        var sale = await saleRepository.GetByAccountAsync(accountId, cancellationToken);

        return sale is null
            ? Result<SaleResponse>.NotFound("sale.not_found", "La cuenta no tiene venta asociada.")
            : ToSaleResponse(sale);
    }

    public async Task<Result<SaleResponse>> CreateAsync(CreateSaleRequest request, CancellationToken cancellationToken)
    {
        var account = await accountRepository.GetByIdAsync(request.TableAccountId, cancellationToken);
        if (account is null)
        {
            return Result<SaleResponse>.NotFound("account.not_found", "La cuenta no existe.");
        }

        if (request.Items is null || request.Items.Count == 0)
        {
            return Result<SaleResponse>.ValidationFailure("sale.no_items", "La venta debe tener al menos un item.");
        }

        var sale = Sale.Create(request.SaleNumber, request.TableAccountId, account.Subtotal, account.Discount, account.Tip);

        foreach (var item in request.Items)
        {
            var product = await productRepository.GetWithDetailsAsync(item.ProductId, cancellationToken);
            if (product is null)
            {
                return Result<SaleResponse>.ValidationFailure("sale.product_not_found", $"El producto {item.ProductId} no existe.");
            }

            sale.AddItem(item.ProductId, product.Name, item.Quantity, product.GetCurrentPrice());
        }

        sale.RecalculateSubtotal();

        try
        {
            await saleRepository.AddAsync(sale, cancellationToken);
            await saleRepository.SaveChangesAsync(cancellationToken);

            return ToSaleResponse(sale);
        }
        catch (DomainException exception)
        {
            return Result<SaleResponse>.ValidationFailure("sale.invalid", exception.Message);
        }
    }

    public async Task<Result<SaleResponse>> AddItemAsync(Guid saleId, SaleItemRequest request, CancellationToken cancellationToken)
    {
        var sale = await saleRepository.GetWithItemsAsync(saleId, cancellationToken);
        if (sale is null)
        {
            return Result<SaleResponse>.NotFound("sale.not_found", "La venta no existe.");
        }

        var product = await productRepository.GetWithDetailsAsync(request.ProductId, cancellationToken);
        if (product is null)
        {
            return Result<SaleResponse>.ValidationFailure("sale.product_not_found", $"El producto {request.ProductId} no existe.");
        }

        sale.AddItem(request.ProductId, product.Name, request.Quantity, product.GetCurrentPrice());
        sale.RecalculateSubtotal();

        await saleRepository.SaveChangesAsync(cancellationToken);

        return ToSaleResponse(sale);
    }

    private static SaleResponse ToSaleResponse(Sale sale) => new(
        sale.Id,
        sale.TableAccountId,
        sale.SaleNumber,
        sale.Subtotal,
        sale.Discount,
        sale.Tip,
        sale.Total,
        sale.PaidAmount,
        Math.Max(sale.Total - sale.PaidAmount, 0m),
        sale.IsFullyPaid,
        sale.InvoiceId,
        sale.Items
            .Select(i => new SaleItemResponse(
                i.Id,
                i.ProductId,
                i.ProductName,
                i.Quantity,
                i.UnitPrice,
                i.LineTotal))
            .ToList());
}
