package cn.tyt.llmproxy.listener;

import cn.tyt.llmproxy.service.IAsyncTaskConsumerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AsyncTaskListener {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskListener.class);

    @Autowired
    private IAsyncTaskConsumerService asyncTaskConsumerService;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = "async.chat.queue")
    public void handleChatTask(Map<String, Object> message) {
        try {
            String jobId = (String) message.get("jobId");
            Integer userId = (Integer) message.get("userId");
            Integer modelId = (Integer) message.get("modelId");
            Map<String, Object> requestPayload = objectMapper.convertValue(message.get("requestPayload"), 
                new TypeReference<Map<String, Object>>() {});
            
            log.info("Received async chat task for jobId: {}, userId: {}", jobId, userId);
            asyncTaskConsumerService.processChatTask(jobId, userId, modelId, requestPayload);
            
        } catch (Exception e) {
            log.error("Failed to process async chat task message: {}", message, e);
        }
    }

    @RabbitListener(queues = "async.image.queue")
    public void handleImageTask(Map<String, Object> message) {
        try {
            String jobId = (String) message.get("jobId");
            Integer userId = (Integer) message.get("userId");
            Integer modelId = (Integer) message.get("modelId");
            Map<String, Object> requestPayload = objectMapper.convertValue(message.get("requestPayload"), 
                new TypeReference<Map<String, Object>>() {});
            
            log.info("Received async image task for jobId: {}, userId: {}", jobId, userId);
            asyncTaskConsumerService.processImageTask(jobId, userId, modelId, requestPayload);
            
        } catch (Exception e) {
            log.error("Failed to process async image task message: {}", message, e);
        }
    }
}
