package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.entity.AsyncJob;
import java.util.Map;

public interface IAsyncJobService {
    
    /**
     * 创建异步任务
     */
    AsyncJob createAsyncJob(Integer userId, Integer modelId, Map<String, Object> requestPayload);
    
    /**
     * 根据任务ID获取任务状态
     */
    AsyncJob getJobStatus(String jobId);
    
    /**
     * 更新任务状态
     */
    boolean updateJobStatus(String jobId, String status, Map<String, Object> resultPayload, String errorMessage);
}
