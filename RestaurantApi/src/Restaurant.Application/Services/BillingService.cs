using Restaurant.Application.Billing;
using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Billing;
using Restaurant.Domain.Common;
using Restaurant.Domain.Sales;

namespace Restaurant.Application.Services;

public sealed class NullElectronicInvoiceProvider : IElectronicInvoiceProvider
{
    public Task<string?> IssueAsync(InvoiceResponse invoice, CancellationToken cancellationToken)
    {
        return Task.FromResult<string?>(null);
    }
}

public sealed class BillingService(
    IInvoiceRepository invoiceRepository,
    ISaleRepository saleRepository,
    IElectronicInvoiceProvider electronicInvoiceProvider) : IBillingService
{
    public async Task<Result<InvoiceResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var invoice = await invoiceRepository.GetWithItemsAsync(id, cancellationToken);

        return invoice is null
            ? Result<InvoiceResponse>.NotFound("invoice.not_found", "La factura no existe.")
            : ToResponse(invoice);
    }

    public async Task<Result<InvoiceResponse>> GetBySaleAsync(Guid saleId, CancellationToken cancellationToken)
    {
        var invoice = await invoiceRepository.GetBySaleAsync(saleId, cancellationToken);

        return invoice is null
            ? Result<InvoiceResponse>.NotFound("invoice.not_found", "La venta no tiene factura asociada.")
            : ToResponse(invoice);
    }

    public async Task<Result<InvoiceResponse>> CreateAsync(CreateInvoiceRequest request, CancellationToken cancellationToken)
    {
        if (request.SaleId is not null)
        {
            var sale = await saleRepository.GetWithItemsAsync(request.SaleId.Value, cancellationToken);
            if (sale is null)
            {
                return Result<InvoiceResponse>.NotFound("sale.not_found", "La venta no existe.");
            }

            var existing = await invoiceRepository.GetBySaleAsync(request.SaleId.Value, cancellationToken);
            if (existing is not null)
            {
                return Result<InvoiceResponse>.Conflict("invoice.already_exists", "La venta ya tiene una factura asociada.");
            }
        }

        try
        {
            var customer = CustomerBillingData.Create(
                request.Customer.Identification,
                request.Customer.TaxId,
                request.Customer.LegalName,
                request.Customer.Address,
                request.Customer.Phone,
                request.Customer.Email,
                request.Customer.IsFinalConsumer);

            var invoice = Invoice.Create(request.InvoiceNumber, customer, request.SaleId, request.TableAccountId);
            invoice.SetTotals(request.Subtotal, request.Discount, request.Tip);

            if (request.Items is not null)
            {
                foreach (var item in request.Items)
                {
                    invoice.AddItem(item.ProductId, item.Description, item.Quantity, item.UnitPrice);
                }
            }

            await invoiceRepository.AddAsync(invoice, cancellationToken);
            await invoiceRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(invoice);
        }
        catch (DomainException exception)
        {
            return Result<InvoiceResponse>.ValidationFailure("invoice.invalid", exception.Message);
        }
    }

    public async Task<Result<InvoiceResponse>> IssueAsync(Guid id, CancellationToken cancellationToken)
    {
        var invoice = await invoiceRepository.GetWithItemsAsync(id, cancellationToken);
        if (invoice is null)
        {
            return Result<InvoiceResponse>.NotFound("invoice.not_found", "La factura no existe.");
        }

        try
        {
            invoice.Issue();
            var authorization = await electronicInvoiceProvider.IssueAsync(ToResponse(invoice), cancellationToken);

            if (!string.IsNullOrWhiteSpace(authorization))
            {
                invoice.Issue(authorization);
            }

            if (invoice.SaleId is not null)
            {
                var sale = await saleRepository.GetByIdAsync(invoice.SaleId.Value, cancellationToken);
                sale?.AttachInvoice(invoice.Id);
            }

            await invoiceRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(invoice);
        }
        catch (DomainException exception)
        {
            return Result<InvoiceResponse>.BusinessRuleFailure("invoice.invalid", exception.Message);
        }
    }

    public async Task<Result<InvoiceResponse>> CancelAsync(Guid id, CancellationToken cancellationToken)
    {
        var invoice = await invoiceRepository.GetWithItemsAsync(id, cancellationToken);
        if (invoice is null)
        {
            return Result<InvoiceResponse>.NotFound("invoice.not_found", "La factura no existe.");
        }

        try
        {
            invoice.Cancel();
            await invoiceRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(invoice);
        }
        catch (DomainException exception)
        {
            return Result<InvoiceResponse>.BusinessRuleFailure("invoice.invalid", exception.Message);
        }
    }

    private static InvoiceResponse ToResponse(Invoice invoice) => new(
        invoice.Id,
        invoice.InvoiceNumber,
        invoice.SaleId,
        invoice.TableAccountId,
        invoice.Status.ToString(),
        invoice.Subtotal,
        invoice.Discount,
        invoice.Tip,
        invoice.Total,
        invoice.AuthorizationNumber,
        invoice.IssuedAtUtc,
        new BillingCustomerResponse(
            invoice.CustomerData.Identification,
            invoice.CustomerData.TaxId,
            invoice.CustomerData.LegalName,
            invoice.CustomerData.Address,
            invoice.CustomerData.Phone,
            invoice.CustomerData.Email,
            invoice.CustomerData.IsFinalConsumer),
        invoice.Items
            .Select(i => new InvoiceItemResponse(
                i.Id,
                i.ProductId,
                i.Description,
                i.Quantity,
                i.UnitPrice,
                i.LineTotal))
            .ToList());
}