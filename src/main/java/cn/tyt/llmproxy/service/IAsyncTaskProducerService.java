package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.entity.AsyncJob;
import java.util.Map;

public interface IAsyncTaskProducerService {
    
    /**
     * 发送异步聊天任务到消息队列
     */
    void sendChatTask(AsyncJob job, Map<String, Object> chatRequest);
    
    /**
     * 发送异步图片生成任务到消息队列
     */
    void sendImageTask(AsyncJob job, Map<String, Object> imageRequest);
}
