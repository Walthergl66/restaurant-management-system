using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Restaurant.Application.Administration;
using Restaurant.Domain.Users;

namespace Restaurant.Api.Controllers;

[ApiController]
[Route("api")]
[Authorize]
public sealed class RolesController(IRoleAdminService roleAdminService) : ControllerBase
{
    [HttpGet("roles")]
    [Authorize(Policy = Permissions.RolesManage)]
    [ProducesResponseType<IReadOnlyCollection<RoleResponse>>(StatusCodes.Status200OK)]
    public async Task<IActionResult> GetRoles(CancellationToken cancellationToken)
    {
        var result = await roleAdminService.GetRolesAsync(cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpGet("permissions")]
    [Authorize(Policy = Permissions.RolesManage)]
    [ProducesResponseType<IReadOnlyCollection<PermissionResponse>>(StatusCodes.Status200OK)]
    public async Task<IActionResult> GetPermissions(CancellationToken cancellationToken)
    {
        var result = await roleAdminService.GetPermissionsAsync(cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpPost("roles/permissions")]
    [Authorize(Policy = Permissions.RolesManage)]
    [ProducesResponseType<RoleResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> GrantPermission([FromBody] GrantPermissionRequest request, CancellationToken cancellationToken)
    {
        var result = await roleAdminService.GrantPermissionAsync(request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpDelete("roles/{role}/permissions/{permission}")]
    [Authorize(Policy = Permissions.RolesManage)]
    [ProducesResponseType<RoleResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> RevokePermission(string role, string permission, CancellationToken cancellationToken)
    {
        var result = await roleAdminService.RevokePermissionAsync(new GrantPermissionRequest(role, permission), cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpPost("users/roles")]
    [Authorize(Policy = Permissions.RolesManage)]
    [ProducesResponseType<EmployeeResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> AssignRole([FromBody] AssignRoleRequest request, CancellationToken cancellationToken)
    {
        var result = await roleAdminService.AssignRoleAsync(request, cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }

    [HttpDelete("users/{userId:guid}/roles/{role}")]
    [Authorize(Policy = Permissions.RolesManage)]
    [ProducesResponseType<EmployeeResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> RemoveRole(Guid userId, string role, CancellationToken cancellationToken)
    {
        var result = await roleAdminService.RemoveRoleAsync(new AssignRoleRequest(userId, role), cancellationToken);

        return result.IsSuccess ? Ok(result.Value) : BadRequest(new { error = result.Error });
    }
}