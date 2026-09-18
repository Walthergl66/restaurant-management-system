using Restaurant.Application.Common;

namespace Restaurant.Application.Cancellations;

public sealed record CancellationRequestResponse(
    Guid Id,
    Guid OrderId,
    Guid ItemId,
    Guid ProductId,
    string ProductName,
    int Quantity,
    Guid? TableId,
    Guid RequestedByUserId,
    string Reason,
    string Status,
    Guid? ReviewedByUserId,
    DateTime? ReviewedAtUtc,
    string? ReviewNote);

public sealed record CreateCancellationRequestRequest(
    Guid ItemId,
    string Reason,
    Guid? TableId);

public sealed record ReviewCancellationRequestRequest(string? Note);

public interface ICancellationService
{
    Task<Result<IReadOnlyCollection<CancellationRequestResponse>>> GetPendingAsync(CancellationToken cancellationToken);

    Task<Result<CancellationRequestResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<CancellationRequestResponse>> CreateAsync(Guid orderId, CreateCancellationRequestRequest request, Guid userId, CancellationToken cancellationToken);

    Task<Result<CancellationRequestResponse>> ApproveAsync(Guid id, ReviewCancellationRequestRequest request, Guid reviewerUserId, CancellationToken cancellationToken);

    Task<Result<CancellationRequestResponse>> RejectAsync(Guid id, ReviewCancellationRequestRequest request, Guid reviewerUserId, CancellationToken cancellationToken);
}