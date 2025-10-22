package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.dto.request.ChatRequest_dto;
import cn.tyt.llmproxy.dto.request.ImageGenerationRequest;
import cn.tyt.llmproxy.dto.response.ChatResponse_dto;
import cn.tyt.llmproxy.dto.response.ImageGenerationResponse;
import cn.tyt.llmproxy.entity.AsyncJob;
import cn.tyt.llmproxy.service.IAsyncTaskConsumerService;
import cn.tyt.llmproxy.service.IAsyncJobService;
import cn.tyt.llmproxy.service.ILangchainProxyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AsyncTaskConsumerServiceImpl implements IAsyncTaskConsumerService {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskConsumerServiceImpl.class);

    @Autowired
    private IAsyncJobService asyncJobService;

    @Autowired
    private ILangchainProxyService langchainProxyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void processChatTask(String jobId, Integer userId, Integer modelId, Map<String, Object> requestPayload) {
        try {
            // 更新任务状态为处理中
            asyncJobService.updateJobStatus(jobId, AsyncJob.STATUS_PROCESSING, null, null);
            
            // 转换请求数据
            ChatRequest_dto chatRequest = objectMapper.convertValue(requestPayload, ChatRequest_dto.class);
            
            // 调用实际的聊天服务
            ChatResponse_dto response = langchainProxyService.chat(chatRequest, userId, null);
            
            // 更新任务状态为已完成
            Map<String, Object> resultPayload = objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {});
            asyncJobService.updateJobStatus(jobId, AsyncJob.STATUS_COMPLETED, resultPayload, null);
            
            log.info("Async chat task completed successfully for jobId: {}", jobId);
            
        } catch (Exception e) {
            log.error("Failed to process async chat task for jobId: {}", jobId, e);
            asyncJobService.updateJobStatus(jobId, AsyncJob.STATUS_FAILED, null, e.getMessage());
        }
    }

    @Override
    public void processImageTask(String jobId, Integer userId, Integer modelId, Map<String, Object> requestPayload) {
        try {
            // 更新任务状态为处理中
            asyncJobService.updateJobStatus(jobId, AsyncJob.STATUS_PROCESSING, null, null);
            
            // 转换请求数据
            ImageGenerationRequest imageRequest = objectMapper.convertValue(requestPayload, ImageGenerationRequest.class);
            
            // 调用实际的图片生成服务
            ImageGenerationResponse response = langchainProxyService.generateImage(imageRequest, userId, null);
            
            // 更新任务状态为已完成
            Map<String, Object> resultPayload = objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {});
            asyncJobService.updateJobStatus(jobId, AsyncJob.STATUS_COMPLETED, resultPayload, null);
            
            log.info("Async image task completed successfully for jobId: {}", jobId);
            
        } catch (Exception e) {
            log.error("Failed to process async image task for jobId: {}", jobId, e);
            asyncJobService.updateJobStatus(jobId, AsyncJob.STATUS_FAILED, null, e.getMessage());
        }
    }
}
