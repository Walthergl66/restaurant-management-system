namespace Restaurant.Domain.Users;

public static class Permissions
{
    public const string OrdersCreate = "orders:create";
    public const string OrdersConfirm = "orders:confirm";
    public const string OrdersCancel = "orders:cancel";
    public const string AdditionsCreate = "additions:create";
    public const string CancellationsRequest = "cancellations:request";
    public const string CancellationsApprove = "cancellations:approve";
    public const string TableAccountsManage = "table-accounts:manage";
    public const string PaymentsCreate = "payments:create";
    public const string CashOpen = "cash:open";
    public const string CashClose = "cash:close";
    public const string ReportsView = "reports:view";
    public const string FinanceView = "finance:view";
    public const string ProductsManage = "products:manage";
    public const string UsersManage = "users:manage";
    public const string RolesManage = "roles:manage";
    public const string SettingsManage = "settings:manage";
    public const string PrinterManage = "printer:manage";
    public const string AuditView = "audit:view";

    public static readonly IReadOnlyList<string> All =
    [
        OrdersCreate,
        OrdersConfirm,
        OrdersCancel,
        AdditionsCreate,
        CancellationsRequest,
        CancellationsApprove,
        TableAccountsManage,
        PaymentsCreate,
        CashOpen,
        CashClose,
        ReportsView,
        FinanceView,
        ProductsManage,
        UsersManage,
        RolesManage,
        SettingsManage,
        PrinterManage,
        AuditView,
    ];
}