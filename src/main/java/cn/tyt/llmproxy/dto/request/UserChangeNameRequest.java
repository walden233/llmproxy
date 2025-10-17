package cn.tyt.llmproxy.dto.request;

import jakarta.validation.constraints.NotBlank;;
import lombok.Data;

@Data
public class UserChangeNameRequest {

    @NotBlank(message = "新用户名不能为空")
    private String newName;
}