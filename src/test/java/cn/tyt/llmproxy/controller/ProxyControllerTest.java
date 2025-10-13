package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.dto.request.UserLoginRequest;
import cn.tyt.llmproxy.dto.request.ChatRequest_dto;
import cn.tyt.llmproxy.dto.request.ImageGenerationRequest;
import cn.tyt.llmproxy.dto.request.ImageInput;
import cn.tyt.llmproxy.dto.response.UserLoginResponse;
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

        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseString = loginResult.getResponse().getContentAsString();
        Result<UserLoginResponse> result = objectMapper.readValue(responseString, new TypeReference<Result<UserLoginResponse>>() {});
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
        chatRequest.setUserMessage("重复一遍");
        List<Map<String,String>> history = new ArrayList<>();
        //测试提供记忆
        history.add(Map.of("role", "user", "content", "你拍一"));
        history.add(Map.of("role", "assistant", "content", "我不拍一"));
        chatRequest.setHistory(history);
        // 不指定 modelIdentifier，让系统自动选择

        MvcResult result = mockMvc.perform(post("/v1/chat")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.data.usedModelIdentifier").value(MOCK_CHAT_MODEL_ID))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        System.out.println("远端大模型回答:"+responseString);
    }

    @Test
    void testChat_Success_With_Options() throws Exception {
        ChatRequest_dto chatRequest = new ChatRequest_dto();
        chatRequest.setUserMessage("重复一遍");
        List<Map<String,String>> history = new ArrayList<>();
        //测试提供记忆
        history.add(Map.of("role", "user", "content", "你拍一"));
        history.add(Map.of("role", "assistant", "content", "我不拍一"));
        chatRequest.setHistory(history);

        chatRequest.setOptions(Map.of(
                "temperature", 0.7,
                "max_tokens", 512,
                "top_p", 0.9,
                "frequency_penalty", 0.0
        ));
        chatRequest.setModelIdentifier("glm-4-plus");
        // 不指定 modelIdentifier，让系统自动选择

        MvcResult result = mockMvc.perform(post("/v1/chat")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.data.usedModelIdentifier").value(MOCK_CHAT_MODEL_ID))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        System.out.println("远端大模型回答:"+responseString);
    }

    @Test
    void testChat_Success_AutoSelectModel_With_ImageUrl() throws Exception {
        ChatRequest_dto chatRequest = new ChatRequest_dto();
        chatRequest.setUserMessage("图上有什么？");
        List<ImageInput> ims = new ArrayList<>();
        ImageInput im = new ImageInput();
        im.setUrl("https://i0.hdslb.com/bfs/archive/fbcca754eadc47994aaaa0964a7ddf366cd8033a.png");
        ims.add(im);
        chatRequest.setImages(ims);

        MvcResult result = mockMvc.perform(post("/v1/chat")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.data.usedModelIdentifier").value(MOCK_CHAT_MODEL_ID))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        System.out.println("远端大模型回答:"+responseString);
    }

    @Test
    void test_GenerateImg_Success() throws Exception {
        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("画个动漫人物");
//        request.setSize("1024*1024");
//        request.setModelInternalId("wanx2.1-t2i-turbo");

        MvcResult result = mockMvc.perform(post("/v1/generate-image")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        System.out.println("远端大模型回答:"+responseString);
    }

    @Test
    void test_EditImg_Success() throws Exception {
        ImageGenerationRequest request = new ImageGenerationRequest();
        ImageInput im = new ImageInput();
        im.setUrl("http://wanx.alicdn.com/material/20250318/stylization_all_1.jpeg");
        request.setOriginImage(im);
        request.setPrompt("把花的颜色替换成白色");
//        request.setModelInternalId("wanx2.1-imageedit");

        MvcResult result = mockMvc.perform(post("/v1/generate-image")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        System.out.println("远端大模型回答:"+responseString);
    }
}

