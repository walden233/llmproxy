package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.entity.AsyncTaskOutbox;

public interface IAsyncTaskOutboxService {

    AsyncTaskOutbox enqueue(String jobId, String exchange, String routingKey, String payload);
}
