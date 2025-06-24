package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.dto.request.AdminLoginRequest;
import cn.tyt.llmproxy.dto.request.ChatRequest_dto;
import cn.tyt.llmproxy.dto.response.AdminLoginResponse;
import cn.tyt.llmproxy.mapper.AdminMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import cn.tyt.llmproxy.common.domain.Result;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;


import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

public class ProxyControllerTest extends BaseTest {
    @Autowired
    private AdminMapper adminMapper;

    private final String MOCK_CHAT_MODEL_ID = "deepseek-chat";
    private String authToken; // JWT Token

    @BeforeEach
    void loginAndGetToken() throws Exception {
        // 确保测试用户存在并登录以获取 token
        String username = "test";
        String password = "password";

        AdminLoginRequest loginRequest = new AdminLoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

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
    void testChat_Success_WithSpecificModel() throws Exception {
        ChatRequest_dto chatRequest = new ChatRequest_dto();
        chatRequest.setUserMessage("Hello!");
        chatRequest.setModelIdentifier(MOCK_CHAT_MODEL_ID); // 指定模型

        MvcResult result = mockMvc.perform(post("/v1/chat")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.usedModelIdentifier").value(MOCK_CHAT_MODEL_ID))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        System.out.println("远端大模型回答:"+responseString);
    }
    @Test
    void testChat_Success_AutoSelectModel() throws Exception {
        ChatRequest_dto chatRequest = new ChatRequest_dto();
        chatRequest.setUserMessage("你拍二");
        List<String> history = new ArrayList<>();
        //测试提供记忆
        history.add("user: 你拍一");
        history.add("ai: 我不拍一");
        chatRequest.setHistory(history);
        // 不指定 modelIdentifier，让系统自动选择

        MvcResult result = mockMvc.perform(post("/v1/chat")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.usedModelIdentifier").value(MOCK_CHAT_MODEL_ID))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        System.out.println("远端大模型回答:"+responseString);
    }
}