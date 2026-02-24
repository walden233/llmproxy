package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.config.RabbitMQConfig;
import cn.tyt.llmproxy.dto.request.UsageLogMessage;
import cn.tyt.llmproxy.service.IUsageLogProducerService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsageLogProducerServiceImpl implements IUsageLogProducerService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void sendUsageLog(UsageLogMessage message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.STATS_EXCHANGE, RabbitMQConfig.STATS_ROUTING_KEY, message);
    }
}
