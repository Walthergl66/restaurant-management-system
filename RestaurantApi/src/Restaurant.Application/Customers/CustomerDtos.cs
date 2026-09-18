using Restaurant.Application.Common;

namespace Restaurant.Application.Customers;

public sealed record AddressResponse(
    Guid Id,
    string Label,
    string Line1,
    string? Line2,
    string City,
    string? Reference,
    bool IsDefault);

public sealed record CustomerProfileResponse(
    Guid Id,
    Guid? UserId,
    string FullName,
    string? Phone,
    string? Email,
    bool IsActive,
    IReadOnlyCollection<AddressResponse> Addresses);

public sealed record CreateCustomerProfileRequest(
    Guid? UserId,
    string FullName,
    string? Phone,
    string? Email);

public sealed record UpdateCustomerProfileRequest(
    string FullName,
    string? Phone,
    string? Email,
    bool IsActive);

public sealed record CreateAddressRequest(
    string Label,
    string Line1,
    string? Line2,
    string City,
    string? Reference,
    bool IsDefault);

public interface ICustomerService
{
    Task<Result<CustomerProfileResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken);

    Task<Result<CustomerProfileResponse>> GetByUserAsync(Guid userId, CancellationToken cancellationToken);

    Task<Result<CustomerProfileResponse>> CreateAsync(CreateCustomerProfileRequest request, CancellationToken cancellationToken);

    Task<Result<CustomerProfileResponse>> UpdateAsync(Guid id, UpdateCustomerProfileRequest request, CancellationToken cancellationToken);

    Task<Result<CustomerProfileResponse>> AddAddressAsync(Guid id, CreateAddressRequest request, CancellationToken cancellationToken);

    Task<Result> RemoveAddressAsync(Guid id, Guid addressId, CancellationToken cancellationToken);
}