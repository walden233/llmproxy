package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.dto.request.UserAssignRoleRequest;
import cn.tyt.llmproxy.dto.response.UserProfileResponse;
import cn.tyt.llmproxy.service.IUserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/v1/users")
public class UserController {

    @Autowired
    private IUserService userService;


    @PostMapping("/assign-role")
    @PreAuthorize("hasAuthority('ROLE_ROOT_ADMIN')")
    public Result<?> assignRole(@Valid @RequestBody UserAssignRoleRequest request) {
        userService.assignRole(request.getUserId(), request.getRole());
        return Result.success("角色分配成功");
    }



    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ROOT_ADMIN')")
    public Result<IPage<UserProfileResponse>> getAllUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String role, // e.g., "ROLE_ROOT_ADMIN"
            @RequestParam(required = false, defaultValue = "id") String sortBy, // e.g., "id", "name", "createdAt"
            @RequestParam(required = false, defaultValue = "asc") String sortOrder // "asc" or "desc"
    ) {
        IPage<UserProfileResponse> page = userService.findAllUsers(pageNum, pageSize, role, sortBy, sortOrder);
        return Result.success(page);
    }
}
