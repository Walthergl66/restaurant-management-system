using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Restaurant.Infrastructure.Migrations
{
    /// <inheritdoc />
    public partial class PreparationModel : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "preparation_orders",
                schema: "restaurant",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    SourceOrderId = table.Column<Guid>(type: "uuid", nullable: false),
                    PreparationAreaId = table.Column<Guid>(type: "uuid", nullable: false),
                    PreparationAreaName = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: false),
                    Status = table.Column<int>(type: "integer", nullable: false),
                    PrintedAtUtc = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    StartedAtUtc = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    ReadyAtUtc = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    CreatedAtUtc = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    UpdatedAtUtc = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    CreatedBy = table.Column<Guid>(type: "uuid", nullable: true),
                    UpdatedBy = table.Column<Guid>(type: "uuid", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_preparation_orders", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "preparation_order_items",
                schema: "restaurant",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    PreparationOrderId = table.Column<Guid>(type: "uuid", nullable: false),
                    SourceOrderItemId = table.Column<Guid>(type: "uuid", nullable: false),
                    ProductId = table.Column<Guid>(type: "uuid", nullable: false),
                    ProductName = table.Column<string>(type: "character varying(150)", maxLength: 150, nullable: false),
                    Quantity = table.Column<int>(type: "integer", nullable: false),
                    Notes = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_preparation_order_items", x => x.Id);
                    table.ForeignKey(
                        name: "FK_preparation_order_items_preparation_orders_PreparationOrder~",
                        column: x => x.PreparationOrderId,
                        principalSchema: "restaurant",
                        principalTable: "preparation_orders",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_preparation_order_items_PreparationOrderId",
                schema: "restaurant",
                table: "preparation_order_items",
                column: "PreparationOrderId");

            migrationBuilder.CreateIndex(
                name: "IX_preparation_orders_SourceOrderId",
                schema: "restaurant",
                table: "preparation_orders",
                column: "SourceOrderId");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "preparation_order_items",
                schema: "restaurant");

            migrationBuilder.DropTable(
                name: "preparation_orders",
                schema: "restaurant");
        }
    }
}
