package cn.tyt.llmproxy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponse {
    private String token;
    private String username;
    // 可以添加其他用户信息，如角色等
}