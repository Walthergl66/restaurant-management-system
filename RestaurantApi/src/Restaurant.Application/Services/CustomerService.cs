using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Application.Customers;
using Restaurant.Domain.Common;
using Restaurant.Domain.Customers;

namespace Restaurant.Application.Services;

public sealed class CustomerService(ICustomerProfileRepository repository) : ICustomerService
{
    public async Task<Result<CustomerProfileResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var customer = await repository.GetWithAddressesAsync(id, cancellationToken);

        return customer is null
            ? Result<CustomerProfileResponse>.NotFound("customer.not_found", "El cliente no existe.")
            : ToResponse(customer);
    }

    public async Task<Result<CustomerProfileResponse>> GetByUserAsync(Guid userId, CancellationToken cancellationToken)
    {
        var customer = await repository.GetByUserAsync(userId, cancellationToken);

        return customer is null
            ? Result<CustomerProfileResponse>.NotFound("customer.not_found", "El usuario no tiene perfil de cliente.")
            : ToResponse(customer);
    }

    public async Task<Result<CustomerProfileResponse>> CreateAsync(CreateCustomerProfileRequest request, CancellationToken cancellationToken)
    {
        try
        {
            var customer = CustomerProfile.Create(request.UserId, request.FullName, request.Phone, request.Email);
            await repository.AddAsync(customer, cancellationToken);
            await repository.SaveChangesAsync(cancellationToken);

            return ToResponse(customer);
        }
        catch (DomainException exception)
        {
            return Result<CustomerProfileResponse>.ValidationFailure("customer.invalid", exception.Message);
        }
    }

    public async Task<Result<CustomerProfileResponse>> UpdateAsync(Guid id, UpdateCustomerProfileRequest request, CancellationToken cancellationToken)
    {
        var customer = await repository.GetWithAddressesAsync(id, cancellationToken);
        if (customer is null)
        {
            return Result<CustomerProfileResponse>.NotFound("customer.not_found", "El cliente no existe.");
        }

        try
        {
            customer.Update(request.FullName, request.Phone, request.Email, request.IsActive);
            await repository.SaveChangesAsync(cancellationToken);

            return ToResponse(customer);
        }
        catch (DomainException exception)
        {
            return Result<CustomerProfileResponse>.ValidationFailure("customer.invalid", exception.Message);
        }
    }

    public async Task<Result<CustomerProfileResponse>> AddAddressAsync(Guid id, CreateAddressRequest request, CancellationToken cancellationToken)
    {
        var customer = await repository.GetWithAddressesAsync(id, cancellationToken);
        if (customer is null)
        {
            return Result<CustomerProfileResponse>.NotFound("customer.not_found", "El cliente no existe.");
        }

        try
        {
            customer.AddAddress(request.Label, request.Line1, request.Line2, request.City, request.Reference, request.IsDefault);
            await repository.SaveChangesAsync(cancellationToken);

            return ToResponse(customer);
        }
        catch (DomainException exception)
        {
            return Result<CustomerProfileResponse>.ValidationFailure("customer.invalid", exception.Message);
        }
    }

    public async Task<Result> RemoveAddressAsync(Guid id, Guid addressId, CancellationToken cancellationToken)
    {
        var customer = await repository.GetWithAddressesAsync(id, cancellationToken);
        if (customer is null)
        {
            return Result.NotFound("customer.not_found", "El cliente no existe.");
        }

        try
        {
            customer.RemoveAddress(addressId);
            await repository.SaveChangesAsync(cancellationToken);

            return Result.Success();
        }
        catch (DomainException exception)
        {
            return Result.BusinessRuleFailure("customer.invalid", exception.Message);
        }
    }

    private static CustomerProfileResponse ToResponse(CustomerProfile customer) => new(
        customer.Id,
        customer.UserId,
        customer.FullName,
        customer.Phone,
        customer.Email,
        customer.IsActive,
        customer.Addresses
            .Select(a => new AddressResponse(a.Id, a.Label, a.Line1, a.Line2, a.City, a.Reference, a.IsDefault))
            .ToList());
}