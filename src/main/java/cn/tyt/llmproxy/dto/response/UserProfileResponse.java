package cn.tyt.llmproxy.dto.response;

import cn.tyt.llmproxy.entity.User;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserProfileResponse {

    private Integer id;
    private String username;
    private String email;
    private String role;
    private BigDecimal balance;

    public static UserProfileResponse fromEntity(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setBalance(user.getBalance());
        return response;
    }
}
