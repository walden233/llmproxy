package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.entity.AsyncJob;
import cn.tyt.llmproxy.service.IAsyncTaskProducerService;
import cn.tyt.llmproxy.service.IAsyncTaskOutboxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AsyncTaskProducerServiceImpl implements IAsyncTaskProducerService {

    @Autowired
    private IAsyncTaskOutboxService asyncTaskOutboxService;
    @Autowired
    private ObjectMapper objectMapper;
    
    // RabbitMQ 配置常量
    public static final String ASYNC_TASK_EXCHANGE = "async.task.exchange";
    public static final String ASYNC_CHAT_QUEUE = "async.chat.queue";
    public static final String ASYNC_IMAGE_QUEUE = "async.image.queue";
    public static final String ASYNC_CHAT_ROUTING_KEY = "async.chat";
    public static final String ASYNC_IMAGE_ROUTING_KEY = "async.image";

    @Override
    public void sendChatTask(AsyncJob job, Map<String, Object> chatRequest) {
        Map<String, Object> message = new HashMap<>();
        message.put("jobId", job.getJobId());
        message.put("userId", job.getUserId());
        message.put("accessKeyId", job.getAccessKeyId());
        message.put("requestPayload", chatRequest);

        enqueueOutbox(job.getJobId(), ASYNC_TASK_EXCHANGE, ASYNC_CHAT_ROUTING_KEY, message);
    }

    @Override
    public void sendImageTask(AsyncJob job, Map<String, Object> imageRequest) {
        Map<String, Object> message = new HashMap<>();
        message.put("jobId", job.getJobId());
        message.put("userId", job.getUserId());
        message.put("accessKeyId", job.getAccessKeyId());
        message.put("requestPayload", imageRequest);

        enqueueOutbox(job.getJobId(), ASYNC_TASK_EXCHANGE, ASYNC_IMAGE_ROUTING_KEY, message);
    }

    private void enqueueOutbox(String jobId, String exchange, String routingKey, Map<String, Object> message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            asyncTaskOutboxService.enqueue(jobId, exchange, routingKey, payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize async task message", e);
        }
    }
}
