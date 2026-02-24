package cn.tyt.llmproxy.listener;

import cn.tyt.llmproxy.dto.request.UsageLogMessage;
import cn.tyt.llmproxy.service.IStatisticsService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class UsageLogListener {

    private static final Logger log = LoggerFactory.getLogger(UsageLogListener.class);

    @Autowired
    private IStatisticsService statisticsService;

    @RabbitListener(queues = "stats.usage.queue", containerFactory = "rabbitListenerContainerFactory")
    public void handleUsageLog(UsageLogMessage message, Message amqpMessage, Channel channel) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        try {
            statisticsService.recordUsageMongo(
                    message.getUserId(),
                    message.getAccessKeyId(),
                    message.getModelId(),
                    message.getPromptTokens(),
                    message.getCompletionTokens(),
                    message.getImageCount(),
                    message.getCost(),
                    message.getTime(),
                    message.isSuccess(),
                    message.getIsAsync()
            );
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process usage log message", e);
            boolean redelivered = amqpMessage.getMessageProperties().isRedelivered();
            channel.basicNack(deliveryTag, false, !redelivered);
        }
    }
}
