using Restaurant.Application.Common;
using Restaurant.Application.Common.Abstractions;
using Restaurant.Domain.Common;
using Restaurant.Domain.Tables;

namespace Restaurant.Application.Tables;

public sealed class TableService(IRestaurantTableRepository tableRepository) : ITableService
{
    public async Task<Result<IReadOnlyCollection<TableResponse>>> GetAllAsync(bool onlyActive, CancellationToken cancellationToken)
    {
        var tables = await tableRepository.GetAllAsync(onlyActive, cancellationToken);

        return tables
            .Select(ToResponse)
            .ToList();
    }

    public async Task<Result<TableResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var table = await tableRepository.GetByIdAsync(id, cancellationToken);

        return table is null
            ? Result<TableResponse>.NotFound("table.not_found", "La mesa no existe.")
            : ToResponse(table);
    }

    public async Task<Result<TableResponse>> CreateAsync(CreateTableRequest request, CancellationToken cancellationToken)
    {
        try
        {
            var table = RestaurantTable.Create(request.Name, request.Capacity, request.Location);
            await tableRepository.AddAsync(table, cancellationToken);
            await tableRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(table);
        }
        catch (DomainException exception)
        {
            return Result<TableResponse>.ValidationFailure("table.invalid", exception.Message);
        }
    }

    public async Task<Result<TableResponse>> UpdateAsync(Guid id, UpdateTableRequest request, CancellationToken cancellationToken)
    {
        var table = await tableRepository.GetByIdAsync(id, cancellationToken);
        if (table is null)
        {
            return Result<TableResponse>.NotFound("table.not_found", "La mesa no existe.");
        }

        try
        {
            table.Update(request.Name, request.Capacity, request.Location);
            await tableRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(table);
        }
        catch (DomainException exception)
        {
            return Result<TableResponse>.ValidationFailure("table.invalid", exception.Message);
        }
    }

    public async Task<Result> SetStatusAsync(Guid id, int tableStatus, CancellationToken cancellationToken)
    {
        if (!Enum.IsDefined(typeof(TableStatus), tableStatus))
        {
            return Result.ValidationFailure("table.invalid_status", "El estado de mesa indicado no es válido.");
        }

        var table = await tableRepository.GetByIdAsync(id, cancellationToken);
        if (table is null)
        {
            return Result.NotFound("table.not_found", "La mesa no existe.");
        }

        table.SetStatus((TableStatus)tableStatus);
        await tableRepository.SaveChangesAsync(cancellationToken);

        return Result.Success();
    }

    private static TableResponse ToResponse(RestaurantTable table) => new(
        table.Id,
        table.Name,
        table.Capacity,
        table.Status.ToString(),
        table.Location,
        table.CurrentAccountId);
}

public sealed class TableAccountService(
    ITableAccountRepository accountRepository,
    IRestaurantTableRepository tableRepository) : ITableAccountService
{
    public async Task<Result<AccountResponse>> GetByIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var account = await accountRepository.GetByIdAsync(id, cancellationToken);

        return account is null
            ? Result<AccountResponse>.NotFound("account.not_found", "La cuenta no existe.")
            : ToResponse(account);
    }

    public async Task<Result<IReadOnlyCollection<AccountResponse>>> GetOpenAsync(CancellationToken cancellationToken)
    {
        var accounts = await accountRepository.GetOpenAccountsAsync(cancellationToken);

        return accounts
            .Select(ToResponse)
            .ToList();
    }

    public async Task<Result<AccountResponse>> OpenAsync(CreateAccountRequest request, CancellationToken cancellationToken)
    {
        if (request.TableId is null)
        {
            return Result<AccountResponse>.ValidationFailure("account.table_required", "La mesa es obligatoria para abrir una cuenta.");
        }

        var table = await tableRepository.GetByIdAsync(request.TableId.Value, cancellationToken);
        if (table is null)
        {
            return Result<AccountResponse>.NotFound("table.not_found", "La mesa no existe.");
        }

        try
        {
            var account = TableAccount.Create(request.AccountNumber, request.TableId);
            account.UpdateTotals(0, 0, 0);
            await accountRepository.AddAsync(account, cancellationToken);
            await accountRepository.SaveChangesAsync(cancellationToken);

            table.Occupy(account.Id);
            await tableRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(account);
        }
        catch (DomainException exception)
        {
            return Result<AccountResponse>.ValidationFailure("account.invalid", exception.Message);
        }
    }

    public async Task<Result<AccountResponse>> UpdateTotalsAsync(Guid id, UpdateAccountTotalsRequest request, CancellationToken cancellationToken)
    {
        var account = await accountRepository.GetByIdAsync(id, cancellationToken);
        if (account is null)
        {
            return Result<AccountResponse>.NotFound("account.not_found", "La cuenta no existe.");
        }

        try
        {
            account.UpdateTotals(request.Subtotal, request.Discount, request.Tip);
            await accountRepository.SaveChangesAsync(cancellationToken);

            return ToResponse(account);
        }
        catch (DomainException exception)
        {
            return Result<AccountResponse>.ValidationFailure("account.invalid", exception.Message);
        }
    }

    public async Task<Result> CloseAccountAsync(Guid id, CancellationToken cancellationToken)
    {
        var account = await accountRepository.GetByIdAsync(id, cancellationToken);
        if (account is null)
        {
            return Result.NotFound("account.not_found", "La cuenta no existe.");
        }

        try
        {
            account.Close();
            await accountRepository.SaveChangesAsync(cancellationToken);

            if (account.TableId is Guid tableId)
            {
                var table = await tableRepository.GetByIdAsync(tableId, cancellationToken);
                if (table is not null)
                {
                    table.Free();
                    await tableRepository.SaveChangesAsync(cancellationToken);
                }
            }

            return Result.Success();
        }
        catch (DomainException exception)
        {
            return Result.BusinessRuleFailure("account.invalid", exception.Message);
        }
    }

    private static AccountResponse ToResponse(TableAccount account) => new(
        account.Id,
        account.AccountNumber,
        account.TableId,
        account.Status.ToString(),
        account.Subtotal,
        account.Discount,
        account.Tip,
        account.Total,
        account.AmountDue,
        account.PaidAmount,
        account.OpenedAt,
        account.ClosedAt);
}