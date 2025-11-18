package cn.tyt.llmproxy.controller; // 确保包名正确

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.dto.request.UserLoginRequest;
import cn.tyt.llmproxy.dto.request.ModelCreateRequest;
import cn.tyt.llmproxy.dto.request.ModelStatusUpdateRequest;
import cn.tyt.llmproxy.dto.request.ModelUpdateRequest;
import cn.tyt.llmproxy.dto.response.UserLoginResponse;
import cn.tyt.llmproxy.dto.response.ModelResponse;
import cn.tyt.llmproxy.entity.LlmModel;
import cn.tyt.llmproxy.mapper.LlmModelMapper;
import cn.tyt.llmproxy.mapper.UserMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;


import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

public class ModelControllerTests extends BaseTest {

    @Autowired
    private LlmModelMapper llmModelMapper;
    @Autowired
    private UserMapper adminMapper; // 用于创建测试用户

    private String authToken; // JWT Token

    @BeforeEach
    void loginAndGetToken() throws Exception {
        // 确保测试用户存在以获取 token
        String username = "test";
        String password = "password";
        String email = username + "@example.com";

//        // 清理可能存在的同名用户
//        adminMapper.delete(new QueryWrapper<cn.tyt.llmproxy.entity.Admin>().eq("username", username));
//
//
//        AdminRegisterRequest registerRequest = new AdminRegisterRequest();
//        registerRequest.setUsername(username);
//        registerRequest.setPassword(password);
//        registerRequest.setEmail(email);
//        mockMvc.perform(post("/v1/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk());

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

    private ModelCreateRequest createSampleModelCreateRequest(String identifierSuffix) {
        ModelCreateRequest createRequest = new ModelCreateRequest();
        createRequest.setDisplayName("Test Model " + identifierSuffix);
        createRequest.setModelIdentifier("test-model-" + identifierSuffix + "-" + UUID.randomUUID().toString().substring(0,8));
        createRequest.setUrlBase("https://api.example.com/v1/test-model-" + identifierSuffix);
        createRequest.setApiKey("sk-testapikey" + identifierSuffix);
        createRequest.setCapabilities(Arrays.asList("text-to-text", "text-to-image"));
        createRequest.setPriority(10);
        return createRequest;
    }
//    @Test
//    @Rollback(value = false)
//    void CreateModel() throws Exception {
//
//        ModelCreateRequest createRequest = new ModelCreateRequest();
//        createRequest.setDisplayName("ALI-WanX");
//        createRequest.setApiKey("sk-2a8a028420b946b4b4bbbce178e554cf");
//        createRequest.setModelIdentifier("wanx2.1-imageedit");
//        createRequest.setCapabilities(Arrays.asList("image-to-image"));
//        createRequest.setPriority(3);
//
//        mockMvc.perform(post("/v1/models")
//                        .header(HttpHeaders.AUTHORIZATION, authToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(createRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(200))
//                .andReturn();
//    }

    @Test
//    @Rollback(value = false)
    void testCreateModel_Success() throws Exception {
        ModelCreateRequest createRequest = createSampleModelCreateRequest("create");

        MvcResult result = mockMvc.perform(post("/v1/models")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.displayName").value(createRequest.getDisplayName()))
                .andExpect(jsonPath("$.data.modelIdentifier").value(createRequest.getModelIdentifier()))
                .andExpect(jsonPath("$.data.status").value(1)) // 默认上线
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        Result<ModelResponse> createdModelResult = objectMapper.readValue(responseString, new TypeReference<Result<ModelResponse>>() {});
        Integer modelId = createdModelResult.getData().getId();

        // 验证数据库
        LlmModel savedModel = llmModelMapper.selectById(modelId);
        assertThat(savedModel).isNotNull();
        assertThat(savedModel.getDisplayName()).isEqualTo(createRequest.getDisplayName());
        assertThat(savedModel.getCapabilities()).containsExactlyInAnyOrderElementsOf(createRequest.getCapabilities());
    }

    @Test
    void testGetModelById_Success() throws Exception {
        ModelCreateRequest createRequest = createSampleModelCreateRequest("get");
        MvcResult createResult = mockMvc.perform(post("/v1/models")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();
        Result<ModelResponse> createdModelResult = objectMapper.readValue(createResult.getResponse().getContentAsString(), new TypeReference<Result<ModelResponse>>() {});
        Integer modelId = createdModelResult.getData().getId();

        mockMvc.perform(get("/v1/models/" + modelId)
                        .header(HttpHeaders.AUTHORIZATION, authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(modelId))
                .andExpect(jsonPath("$.data.displayName").value(createRequest.getDisplayName()));
    }

    @Test
    void testGetAllModels_Success() throws Exception {
        // 创建几个模型用于测试分页和列表
        mockMvc.perform(post("/v1/models").header(HttpHeaders.AUTHORIZATION, authToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(createSampleModelCreateRequest("list1"))));
        mockMvc.perform(post("/v1/models").header(HttpHeaders.AUTHORIZATION, authToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(createSampleModelCreateRequest("list2"))));

        MvcResult result = mockMvc.perform(get("/v1/models")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .param("pageNum", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").isNumber()) // 或 .isvalue(greaterThanOrEqualTo(2)))
                .andReturn();
        // 可以进一步解析 $.data.records 并验证内容
        String responseString = result.getResponse().getContentAsString();
        Result<Page<ModelResponse>> pageResult = objectMapper.readValue(responseString, new TypeReference<Result<Page<ModelResponse>>>() {});
        assertThat(pageResult.getData().getRecords()).isNotEmpty();
    }


    @Test
    void testUpdateModel_Success() throws Exception {
        ModelCreateRequest createRequest = createSampleModelCreateRequest("update");
        MvcResult createResult = mockMvc.perform(post("/v1/models")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();
        Result<ModelResponse> createdModelResult = objectMapper.readValue(createResult.getResponse().getContentAsString(), new TypeReference<Result<ModelResponse>>() {});
        Integer modelId = createdModelResult.getData().getId();

        ModelUpdateRequest updateRequest = new ModelUpdateRequest();
        updateRequest.setDisplayName("Updated Test Model");
        updateRequest.setPriority(5);
        updateRequest.setCapabilities(List.of("text-to-text")); // 更新能力
        // updateRequest.setApiKey("sk-updatedapikey"); // 测试更新 API Key

        mockMvc.perform(put("/v1/models/" + modelId)
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.displayName").value("Updated Test Model"))
                .andExpect(jsonPath("$.data.priority").value(5))
                .andExpect(jsonPath("$.data.capabilities[0]").value("text-to-text"));

        LlmModel updatedModel = llmModelMapper.selectById(modelId);
        assertThat(updatedModel.getDisplayName()).isEqualTo("Updated Test Model");
        assertThat(updatedModel.getPriority()).isEqualTo(5);
        // assertThat(updatedModel.getApiKey()).isEqualTo("sk-updatedapikey");
    }

    @Test
    void testUpdateModelStatus_Success() throws Exception {
        ModelCreateRequest createRequest = createSampleModelCreateRequest("status");
        MvcResult createResult = mockMvc.perform(post("/v1/models")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();
        Result<ModelResponse> createdModelResult = objectMapper.readValue(createResult.getResponse().getContentAsString(), new TypeReference<Result<ModelResponse>>() {});
        Integer modelId = createdModelResult.getData().getId();

        ModelStatusUpdateRequest statusRequest = new ModelStatusUpdateRequest();
        statusRequest.setStatus(0); // 下线

        mockMvc.perform(post("/v1/models/" + modelId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(0));

        LlmModel modelAfterUpdate = llmModelMapper.selectById(modelId);
        assertThat(modelAfterUpdate.getStatus()).isEqualTo(0);
    }

    @Test
    void testDeleteModel_Success() throws Exception {
        ModelCreateRequest createRequest = createSampleModelCreateRequest("delete");
        MvcResult createResult = mockMvc.perform(post("/v1/models")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();
        Result<ModelResponse> createdModelResult = objectMapper.readValue(createResult.getResponse().getContentAsString(), new TypeReference<Result<ModelResponse>>() {});
        Integer modelId = createdModelResult.getData().getId();

        mockMvc.perform(delete("/v1/models/" + modelId)
                        .header(HttpHeaders.AUTHORIZATION, authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("模型删除成功"));

        LlmModel deletedModel = llmModelMapper.selectById(modelId);
        assertThat(deletedModel).isNull();
    }

    @Test
    void testCreateModel_Unauthorized() throws Exception {
        ModelCreateRequest createRequest = createSampleModelCreateRequest("unauth");
        mockMvc.perform(post("/v1/models")
                        // No Authorization header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden()); // Spring Security 默认返回 403 (或401，取决于你的配置)
    }
}