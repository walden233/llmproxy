package cn.tyt.llmproxy;

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.controller.BaseTest;
import cn.tyt.llmproxy.dto.request.AdminLoginRequest;
import cn.tyt.llmproxy.dto.request.AdminRegisterRequest;
import cn.tyt.llmproxy.dto.request.ModelCreateRequest;
import cn.tyt.llmproxy.dto.response.AdminLoginResponse;
import cn.tyt.llmproxy.entity.Admin;
import cn.tyt.llmproxy.mapper.AdminMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//先把mysql配置好，表建好，然后runtest来填充数据
@SpringBootTest
public class InitDB extends BaseTest {
    private final String testUsername = "test";
    private final String testPassword = "password";
    private final String testEmail = testUsername + "@example.com";
    private String authToken; // JWT Token

    @Test
    @Rollback(value = false)
    void registerAndGetToken() throws Exception {
        //注册
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


        //获取token
        AdminLoginRequest loginRequest = new AdminLoginRequest();
        loginRequest.setUsername(testUsername);
        loginRequest.setPassword(testPassword);
        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String responseString = loginResult.getResponse().getContentAsString();
        Result<AdminLoginResponse> result = objectMapper.readValue(responseString, new TypeReference<Result<AdminLoginResponse>>() {});
        this.authToken = "Bearer " + result.getData().getToken();
    }


    @Test
    @Rollback(value = false)
    void creatModel() throws Exception {
        //获取token
        AdminLoginRequest loginRequest = new AdminLoginRequest();
        loginRequest.setUsername(testUsername);
        loginRequest.setPassword(testPassword);
        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String responseString = loginResult.getResponse().getContentAsString();
        Result<AdminLoginResponse> result = objectMapper.readValue(responseString, new TypeReference<Result<AdminLoginResponse>>() {});
        this.authToken = "Bearer " + result.getData().getToken();


        //创建模型
        ModelCreateRequest createRequest = new ModelCreateRequest();
        createRequest.setDisplayName("GLM-4V");
        createRequest.setApiKey("58355463efa249c1a1f85d17315dc094.b645T7wfP26jGI2B");
        createRequest.setModelIdentifier("glm-4v-plus-0111");
        createRequest.setUrlBase("https://open.bigmodel.cn/api/paas/v4");
        createRequest.setCapabilities(Arrays.asList("text-to-text","image-to-text"));
        createRequest.setPriority(6);

        MvcResult result2 = mockMvc.perform(post("/v1/models")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        createRequest.setDisplayName("GLM-4");
        createRequest.setApiKey("58355463efa249c1a1f85d17315dc094.b645T7wfP26jGI2B");
        createRequest.setModelIdentifier("glm-4-plus");
        createRequest.setUrlBase("https://open.bigmodel.cn/api/paas/v4");
        createRequest.setCapabilities(Arrays.asList("text-to-text"));
        createRequest.setPriority(6);

        result2 = mockMvc.perform(post("/v1/models")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        createRequest.setDisplayName("deepseek-v3");
        createRequest.setApiKey("sk-6bf312e5ab8f4c099439ba1a1080da8e");
        createRequest.setModelIdentifier("deepseek-chat");
        createRequest.setUrlBase("https://api.deepseek.com/v1");
        createRequest.setCapabilities(Arrays.asList("text-to-text"));
        createRequest.setPriority(5);

        result2 = mockMvc.perform(post("/v1/models")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        createRequest.setDisplayName("ALI-WanX");
        createRequest.setApiKey("sk-2a8a028420b946b4b4bbbce178e554cf");
        createRequest.setModelIdentifier("wanx2.1-t2i-turbo");
        createRequest.setUrlBase(null);
        createRequest.setCapabilities(Arrays.asList("text-to-image"));
        createRequest.setPriority(3);

        result2 = mockMvc.perform(post("/v1/models")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        createRequest.setDisplayName("ALI-WanX");
        createRequest.setApiKey("sk-2a8a028420b946b4b4bbbce178e554cf");
        createRequest.setModelIdentifier("wanx2.1-imageedit");
        createRequest.setUrlBase(null);
        createRequest.setCapabilities(Arrays.asList("image-to-image"));
        createRequest.setPriority(3);

        result2 = mockMvc.perform(post("/v1/models")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
    }
}
