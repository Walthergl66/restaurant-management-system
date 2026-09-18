using Restaurant.Application.Cancellations;
using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Cancellations;
using Restaurant.Domain.Common;

namespace Restaurant.Application.Services;

public sealed class CancellationService(
    ICancellationRequestRepository repository,
    IOrderRepository orderRepository,
    ITableAccountRepository accountRepository) : ICancellationService
{
    public async Task<Result<IReadOnlyCollection<CancellationRequestResponse>>> GetPendingAsync(CancellationToken cancellationToken)
    {
        var requests = await repository.GetPendingAsync(cancellationToken);

        return requests
            .Select(ToResponse)
            .ToList();
    }

    public async Task<Result<CancellationRequestResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var request = await repository.GetByIdAsync(id, cancellationToken);

        return request is null
            ? Result<CancellationRequestResponse>.NotFound("cancellation.not_found", "La solicitud de anulación no existe.")
            : ToResponse(request);
    }

    public async Task<Result<CancellationRequestResponse>> CreateAsync(Guid orderId, CreateCancellationRequestRequest request, Guid userId, CancellationToken cancellationToken)
    {
        var order = await orderRepository.GetWithItemsAsync(orderId, cancellationToken);
        if (order is null)
        {
            return Result<CancellationRequestResponse>.NotFound("order.not_found", "El pedido no existe.");
        }

        if (order.Status == Domain.Orders.OrderStatus.DRAFT)
        {
            return Result<CancellationRequestResponse>.ValidationFailure(
                "cancellation.draft_not_allowed",
                "Un pedido DRAFT se modifica directamente, no requiere anulación.");
        }

        var item = order.Items.FirstOrDefault(i => i.Id == request.ItemId);
        if (item is null)
        {
            return Result<CancellationRequestResponse>.ValidationFailure("cancellation.item_not_found", "El item no pertenece al pedido.");
        }

        try
        {
            var cancellation = CancellationRequest.Create(
                orderId,
                item.Id,
                item.ProductId,
                item.ProductName,
                item.Quantity,
                userId,
                request.Reason,
                request.TableId);

            await repository.AddAsync(cancellation, cancellationToken);
            await repository.SaveChangesAsync(cancellationToken);

            return ToResponse(cancellation);
        }
        catch (DomainException exception)
        {
            return Result<CancellationRequestResponse>.ValidationFailure("cancellation.invalid", exception.Message);
        }
    }

    public async Task<Result<CancellationRequestResponse>> ApproveAsync(Guid id, ReviewCancellationRequestRequest request, Guid reviewerUserId, CancellationToken cancellationToken)
    {
        var cancellation = await repository.GetByIdAsync(id, cancellationToken);
        if (cancellation is null)
        {
            return Result<CancellationRequestResponse>.NotFound("cancellation.not_found", "La solicitud de anulación no existe.");
        }

        try
        {
            cancellation.Approve(reviewerUserId, request.Note);
            await ApplyOrderAdjustmentAsync(cancellation, cancellationToken);
            await repository.SaveChangesAsync(cancellationToken);

            return ToResponse(cancellation);
        }
        catch (DomainException exception)
        {
            return Result<CancellationRequestResponse>.BusinessRuleFailure("cancellation.invalid", exception.Message);
        }
    }

    public async Task<Result<CancellationRequestResponse>> RejectAsync(Guid id, ReviewCancellationRequestRequest request, Guid reviewerUserId, CancellationToken cancellationToken)
    {
        var cancellation = await repository.GetByIdAsync(id, cancellationToken);
        if (cancellation is null)
        {
            return Result<CancellationRequestResponse>.NotFound("cancellation.not_found", "La solicitud de anulación no existe.");
        }

        try
        {
            cancellation.Reject(reviewerUserId, request.Note);
            await repository.SaveChangesAsync(cancellationToken);

            return ToResponse(cancellation);
        }
        catch (DomainException exception)
        {
            return Result<CancellationRequestResponse>.BusinessRuleFailure("cancellation.invalid", exception.Message);
        }
    }

    private async Task ApplyOrderAdjustmentAsync(CancellationRequest cancellation, CancellationToken cancellationToken)
    {
        if (cancellation.OrderId == Guid.Empty)
        {
            return;
        }

        var order = await orderRepository.GetWithItemsAsync(cancellation.OrderId, cancellationToken);
        if (order is null || order.AccountId is null)
        {
            return;
        }

        var account = await accountRepository.GetByIdAsync(order.AccountId.Value, cancellationToken);
        if (account is null)
        {
            return;
        }

        var item = order.Items.FirstOrDefault(i => i.Id == cancellation.ItemId);
        if (item is null)
        {
            return;
        }

        account.UpdateTotals(
            Math.Max(0, account.Subtotal - item.LineTotal),
            account.Discount,
            account.Tip);
    }

    private static CancellationRequestResponse ToResponse(CancellationRequest request) => new(
        request.Id,
        request.OrderId,
        request.ItemId,
        request.ProductId,
        request.ProductName,
        request.Quantity,
        request.TableId,
        request.RequestedByUserId,
        request.Reason,
        request.Status.ToString(),
        request.ReviewedByUserId,
        request.ReviewedAtUtc,
        request.ReviewNote);
}