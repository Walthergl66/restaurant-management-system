using System;
using System.Collections.Generic;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Restaurant.Infrastructure.Migrations
{
    /// <inheritdoc />
    public partial class SettingsModel : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "printer_settings",
                schema: "restaurant",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    PreparationAreaId = table.Column<Guid>(type: "uuid", nullable: false),
                    PrinterId = table.Column<Guid>(type: "uuid", nullable: false),
                    Copies = table.Column<int>(type: "integer", nullable: false),
                    IsActive = table.Column<bool>(type: "boolean", nullable: false),
                    CreatedAtUtc = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    UpdatedAtUtc = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    CreatedBy = table.Column<Guid>(type: "uuid", nullable: true),
                    UpdatedBy = table.Column<Guid>(type: "uuid", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_printer_settings", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "restaurant_settings",
                schema: "restaurant",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    RestaurantName = table.Column<string>(type: "character varying(200)", maxLength: 200, nullable: false),
                    LegalName = table.Column<string>(type: "character varying(200)", maxLength: 200, nullable: true),
                    TaxId = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: true),
                    Address = table.Column<string>(type: "character varying(300)", maxLength: 300, nullable: true),
                    Phone = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: true),
                    Email = table.Column<string>(type: "character varying(200)", maxLength: 200, nullable: true),
                    Currency = table.Column<string>(type: "character varying(10)", maxLength: 10, nullable: false),
                    TimeZone = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: false),
                    AllowDineIn = table.Column<bool>(type: "boolean", nullable: false),
                    AllowPickup = table.Column<bool>(type: "boolean", nullable: false),
                    AllowDelivery = table.Column<bool>(type: "boolean", nullable: false),
                    DefaultTaxRate = table.Column<decimal>(type: "numeric(5,2)", precision: 5, scale: 2, nullable: false),
                    Payment_AllowSplitPayment = table.Column<bool>(type: "boolean", nullable: false),
                    Payment_RequirePaidBeforeInvoice = table.Column<bool>(type: "boolean", nullable: false),
                    Payment_TipSuggestionPercent = table.Column<decimal>(type: "numeric(5,2)", precision: 5, scale: 2, nullable: false),
                    Payment_AcceptedMethods = table.Column<List<string>>(type: "text[]", nullable: false),
                    Cancellation_RequireSupervisorApproval = table.Column<bool>(type: "boolean", nullable: false),
                    Cancellation_AllowAfterConfirmation = table.Column<bool>(type: "boolean", nullable: false),
                    Cancellation_MaxMinutesAfterConfirmation = table.Column<int>(type: "integer", nullable: true),
                    Cancellation_RequireReason = table.Column<bool>(type: "boolean", nullable: false),
                    CreatedAtUtc = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    UpdatedAtUtc = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    CreatedBy = table.Column<Guid>(type: "uuid", nullable: true),
                    UpdatedBy = table.Column<Guid>(type: "uuid", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_restaurant_settings", x => x.Id);
                });

            migrationBuilder.CreateIndex(
                name: "IX_printer_settings_PreparationAreaId",
                schema: "restaurant",
                table: "printer_settings",
                column: "PreparationAreaId",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "printer_settings",
                schema: "restaurant");

            migrationBuilder.DropTable(
                name: "restaurant_settings",
                schema: "restaurant");
        }
    }
}
