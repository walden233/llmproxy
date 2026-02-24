package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.dto.request.UsageLogMessage;

public interface IUsageLogProducerService {
    void sendUsageLog(UsageLogMessage message);
}
