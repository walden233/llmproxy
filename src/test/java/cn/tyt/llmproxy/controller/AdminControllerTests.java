package cn.tyt.llmproxy.controller; // 确保包名正确

import cn.tyt.llmproxy.dto.request.AdminLoginRequest;
import cn.tyt.llmproxy.dto.request.AdminRegisterRequest;
import cn.tyt.llmproxy.dto.response.AdminLoginResponse;
import cn.tyt.llmproxy.entity.Admin;
import cn.tyt.llmproxy.mapper.AdminMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import cn.tyt.llmproxy.common.domain.Result; // 确保 Result 类的路径正确
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

public class AdminControllerTests extends BaseTest {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String testUsername = "test" + System.currentTimeMillis();
    private final String testPassword = "password";
    private final String testEmail = testUsername + "@example.com";

    @BeforeEach
    void setUp() {
        // 清理可能存在的同名用户，虽然 @Transactional 会回滚，但为了独立性可以加上
        // adminMapper.delete(new QueryWrapper<Admin>().eq("username", testUsername));
    }

    @Test
//    @Rollback(value = false)
    void testRegisterAdmin_Success() throws Exception {
        AdminRegisterRequest registerRequest = new AdminRegisterRequest();
        registerRequest.setUsername(testUsername);
        registerRequest.setPassword(testPassword);
        registerRequest.setEmail(testEmail);

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("用户注册成功"))
                .andExpect(jsonPath("$.data").value(testUsername));

        // 验证数据库
        Admin savedAdmin = adminMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Admin>().eq("username", testUsername));
        assertThat(savedAdmin).isNotNull();
        assertThat(savedAdmin.getEmail()).isEqualTo(testEmail);
        assertThat(passwordEncoder.matches(testPassword, savedAdmin.getPasswordHash())).isTrue();
    }

    @Test
    void testRegisterAdmin_UsernameAlreadyExists() throws Exception {
        // 先注册一个用户
        AdminRegisterRequest initialRequest = new AdminRegisterRequest();
        initialRequest.setUsername(testUsername);
        initialRequest.setPassword(testPassword);
        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initialRequest)));

        // 尝试用相同的用户名再次注册
        AdminRegisterRequest duplicateRequest = new AdminRegisterRequest();
        duplicateRequest.setUsername(testUsername);
        duplicateRequest.setPassword("anotherPassword");

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest()) // 全局异常处理器返回400
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }


    @Test
    void testLoginAdmin_Success() throws Exception {
        // 准备用户数据
        AdminRegisterRequest registerRequest = new AdminRegisterRequest();
        registerRequest.setUsername(testUsername);
        registerRequest.setPassword(testPassword);
        registerRequest.setEmail(testEmail);
        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));


        AdminLoginRequest loginRequest = new AdminLoginRequest();
        loginRequest.setUsername(testUsername);
        loginRequest.setPassword(testPassword);

        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value(testUsername))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        // 提取 token 用于后续测试
        String responseString = result.getResponse().getContentAsString();
        Result<AdminLoginResponse> loginResult = objectMapper.readValue(responseString, new TypeReference<Result<AdminLoginResponse>>() {});
        assertThat(loginResult.getData().getToken()).isNotBlank();
        // this.adminToken = loginResult.getData().getToken(); // 可以保存到基类或本类成员变量
    }

    @Test
    void testLoginAdmin_IncorrectPassword() throws Exception {
        // 准备用户数据
        AdminRegisterRequest registerRequest = new AdminRegisterRequest();
        registerRequest.setUsername(testUsername);
        registerRequest.setPassword(testPassword);
        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        AdminLoginRequest loginRequest = new AdminLoginRequest();
        loginRequest.setUsername(testUsername);
        loginRequest.setPassword("wrongPassword");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest()) // 全局异常处理器返回400
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void testLoginAdmin_UserNotFound() throws Exception {
        AdminLoginRequest loginRequest = new AdminLoginRequest();
        loginRequest.setUsername("nonexistentuser" + System.currentTimeMillis());
        loginRequest.setPassword("anypassword");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误")); // AdminServiceImpl 中抛出的是 "用户名或密码错误"
    }
}