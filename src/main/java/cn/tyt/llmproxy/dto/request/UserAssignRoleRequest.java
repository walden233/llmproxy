package cn.tyt.llmproxy.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UserAssignRoleRequest {

    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    @NotBlank(message = "角色不能为空")
    private String role;
}