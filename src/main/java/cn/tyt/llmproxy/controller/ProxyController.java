package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.dto.request.ChatRequest_dto;
import cn.tyt.llmproxy.dto.request.ImageGenerationRequest;
import cn.tyt.llmproxy.dto.response.ChatResponse_dto;
import cn.tyt.llmproxy.dto.response.ImageGenerationResponse;
import cn.tyt.llmproxy.entity.AsyncJob;
import cn.tyt.llmproxy.entity.User;
import cn.tyt.llmproxy.filter.AccessKeyInterceptor;
import cn.tyt.llmproxy.service.IAsyncJobService;
import cn.tyt.llmproxy.service.IAsyncTaskProducerService;
import cn.tyt.llmproxy.service.ILangchainProxyService;
import cn.tyt.llmproxy.service.IUserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class ProxyController {

    @Autowired
    private ILangchainProxyService langchainProxyService;

    @Autowired
    private IAsyncJobService asyncJobService;

    @Autowired
    private IAsyncTaskProducerService asyncTaskProducerService;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private IUserService userService;

    @PostMapping("/chat")
    public Result<ChatResponse_dto> chat(@Valid @RequestBody ChatRequest_dto request,
                                         @RequestAttribute(AccessKeyInterceptor.USER_ID) Integer userId,
                                         @RequestAttribute(AccessKeyInterceptor.ACCESS_KEY_ID) Integer accessKeyId) {
        ChatResponse_dto response = langchainProxyService.chat(request, userId, accessKeyId,false);
        return Result.success(response);
    }

    @PostMapping("/generate-image")
    public Result<ImageGenerationResponse> generateImage(@Valid @RequestBody ImageGenerationRequest request,
                                                         @RequestAttribute(AccessKeyInterceptor.USER_ID) Integer userId,
                                                         @RequestAttribute(AccessKeyInterceptor.ACCESS_KEY_ID) Integer accessKeyId) {
        ImageGenerationResponse response = langchainProxyService.generateImage(request, userId, accessKeyId,false);
        return Result.success(response);
    }

    /**
     * 异步聊天接口
     */
    @PostMapping("/async/chat")
    public Result<Map<String, String>> asyncChat(@Valid @RequestBody ChatRequest_dto request,
                                                 @RequestAttribute(AccessKeyInterceptor.USER_ID) Integer userId,
                                                 @RequestAttribute(AccessKeyInterceptor.ACCESS_KEY_ID) Integer accessKeyId) {
        try {
            // 创建异步任务
            Map<String, Object> requestPayload = objectMapper.convertValue(request, new TypeReference<Map<String, Object>>() {});
            AsyncJob job = asyncJobService.createAsyncJob(userId, accessKeyId, requestPayload);
            
            // 发送任务到消息队列
            asyncTaskProducerService.sendChatTask(job, requestPayload);
            
            // 返回任务ID
            Map<String, String> response = new HashMap<>();
            response.put("jobId", job.getJobId());
            response.put("status", "PENDING");
            response.put("message", "Task submitted successfully");
            
            return Result.success(response);
            
        } catch (Exception e) {
            return Result.error("Failed to submit async chat task: " + e.getMessage());
        }
    }

    /**
     * 异步图片生成接口
     */
    @PostMapping("/async/generate-image")
    public Result<Map<String, String>> asyncGenerateImage(@Valid @RequestBody ImageGenerationRequest request,
                                                          @RequestAttribute(AccessKeyInterceptor.USER_ID) Integer userId,
                                                          @RequestAttribute(AccessKeyInterceptor.ACCESS_KEY_ID) Integer accessKeyId) {
        try {
            // 创建异步任务
            Map<String, Object> requestPayload = objectMapper.convertValue(request, new TypeReference<Map<String, Object>>() {});
            AsyncJob job = asyncJobService.createAsyncJob(userId, accessKeyId, requestPayload);
            
            // 发送任务到消息队列
            asyncTaskProducerService.sendImageTask(job, requestPayload);
            
            // 返回任务ID
            Map<String, String> response = new HashMap<>();
            response.put("jobId", job.getJobId());
            response.put("status", "PENDING");
            response.put("message", "Task submitted successfully");
            
            return Result.success(response);
            
        } catch (Exception e) {
            return Result.error("Failed to submit async image generation task: " + e.getMessage());
        }
    }

    /**
     * 查询异步任务状态
     */
    @GetMapping("/async/jobs/{jobId}")
    public Result<AsyncJob> getJobStatus(@PathVariable String jobId) {
        try {
            User user = userService.getCurrentUser();
            AsyncJob job = asyncJobService.getJobStatus(jobId);
            if (job == null) {
                return Result.error("Job not found");
            }
            if (user.getId().equals(job.getUserId()))
                return Result.success(job);
            else
                return Result.error("无权限");
        } catch (Exception e) {
            return Result.error("Failed to get job status: " + e.getMessage());
        }
    }
}
