package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.entity.AsyncTaskOutbox;
import cn.tyt.llmproxy.mapper.AsyncTaskOutboxMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AsyncTaskOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskOutboxPublisher.class);

    private final AsyncTaskOutboxMapper asyncTaskOutboxMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void publishPending() {
        LocalDateTime now = LocalDateTime.now();
        List<AsyncTaskOutbox> pending = asyncTaskOutboxMapper.selectList(
                new LambdaQueryWrapper<AsyncTaskOutbox>()
                        .eq(AsyncTaskOutbox::getStatus, AsyncTaskOutbox.STATUS_PENDING)
                        .le(AsyncTaskOutbox::getNextRetryAt, now)
                        .last("limit 100")
        );
        for (AsyncTaskOutbox outbox : pending) {
            try {
                Object payload = objectMapper.readValue(outbox.getPayload(), Object.class);
                rabbitTemplate.convertAndSend(outbox.getExchange(), outbox.getRoutingKey(), payload);
                outbox.setStatus(AsyncTaskOutbox.STATUS_SENT);
                outbox.setUpdatedAt(LocalDateTime.now());
                asyncTaskOutboxMapper.updateById(outbox);
            } catch (Exception e) {
                int retry = outbox.getRetryCount() == null ? 0 : outbox.getRetryCount();
                outbox.setRetryCount(retry + 1);
                outbox.setNextRetryAt(LocalDateTime.now().plusSeconds(Math.min(60, (retry + 1) * 5L)));
                outbox.setUpdatedAt(LocalDateTime.now());
                asyncTaskOutboxMapper.updateById(outbox);
                log.warn("Failed to publish async outbox id={}, jobId={}, retry={}", outbox.getId(), outbox.getJobId(), retry + 1, e);
            }
        }
    }
}
