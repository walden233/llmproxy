package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.entity.AsyncTaskOutbox;
import cn.tyt.llmproxy.mapper.AsyncTaskOutboxMapper;
import cn.tyt.llmproxy.service.IAsyncTaskOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AsyncTaskOutboxServiceImpl implements IAsyncTaskOutboxService {

    private final AsyncTaskOutboxMapper asyncTaskOutboxMapper;

    @Override
    @Transactional
    public AsyncTaskOutbox enqueue(String jobId, String exchange, String routingKey, String payload) {
        AsyncTaskOutbox outbox = new AsyncTaskOutbox();
        outbox.setJobId(jobId);
        outbox.setExchange(exchange);
        outbox.setRoutingKey(routingKey);
        outbox.setPayload(payload);
        outbox.setStatus(AsyncTaskOutbox.STATUS_PENDING);
        outbox.setRetryCount(0);
        outbox.setNextRetryAt(LocalDateTime.now());
        outbox.setCreatedAt(LocalDateTime.now());
        outbox.setUpdatedAt(LocalDateTime.now());
        asyncTaskOutboxMapper.insert(outbox);
        return outbox;
    }
}
