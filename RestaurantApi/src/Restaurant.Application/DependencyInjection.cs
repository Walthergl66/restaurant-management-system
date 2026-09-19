using Microsoft.Extensions.DependencyInjection;
using Restaurant.Application.Additions;
using Restaurant.Application.Administration;
using Restaurant.Application.Auth;
using Restaurant.Application.Billing;
using Restaurant.Application.Cancellations;
using Restaurant.Application.Cash;
using Restaurant.Application.Finance;
using Restaurant.Application.Catalog;
using Restaurant.Application.Customers;
using Restaurant.Application.Orders;
using Restaurant.Application.Payments;
using Restaurant.Application.Preparation;
using Restaurant.Application.Printing;
using Restaurant.Application.Services;
using Restaurant.Application.Settings;
using Restaurant.Application.Tables;

namespace Restaurant.Application;

public static class DependencyInjection
{
    public static IServiceCollection AddApplication(this IServiceCollection services)
    {
        services.AddScoped<IAuthService, AuthService>();
        services.AddScoped<ICategoryService, CategoryService>();
        services.AddScoped<IPreparationAreaService, PreparationAreaService>();
        services.AddScoped<IExtraService, ExtraService>();
        services.AddScoped<IProductService, ProductService>();
        services.AddScoped<ITableService, TableService>();
        services.AddScoped<ITableAccountService, TableAccountService>();
        services.AddScoped<IOrderService, OrderService>();
        services.AddScoped<IAdditionService, AdditionService>();
        services.AddScoped<ICancellationService, CancellationService>();
        services.AddScoped<IPreparationOrderService, PreparationOrderService>();
        services.AddScoped<IPrinterService, PrinterService>();
        services.AddScoped<IPrintJobService, PrintJobService>();
        services.AddScoped<IPaymentService, PaymentService>();
        services.AddScoped<ISaleService, SaleService>();
        services.AddSingleton<IElectronicInvoiceProvider, NullElectronicInvoiceProvider>();
        services.AddScoped<IBillingService, BillingService>();
        services.AddScoped<ICashService, CashService>();
        services.AddScoped<IFinanceService, FinanceService>();
        services.AddScoped<ICustomerService, CustomerService>();
        services.AddScoped<IEmployeeService, EmployeeService>();
        services.AddScoped<IRoleAdminService, RoleAdminService>();
        services.AddScoped<ISettingsService, SettingsService>();

        return services;
    }
}