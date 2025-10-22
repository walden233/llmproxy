package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.entity.AsyncJob;
import java.util.Map;

public interface IAsyncTaskConsumerService {
    
    /**
     * 处理异步聊天任务
     */
    void processChatTask(String jobId, Integer userId, Integer modelId, Map<String, Object> requestPayload);
    
    /**
     * 处理异步图片生成任务
     */
    void processImageTask(String jobId, Integer userId, Integer modelId, Map<String, Object> requestPayload);
}
