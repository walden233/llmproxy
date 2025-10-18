package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.dto.UsageLogDocument;
import cn.tyt.llmproxy.repository.UsageLogRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsageLogService {

    private final UsageLogRepository usageLogRepository;

    // Constructor injection is the recommended way to inject dependencies
    public UsageLogService(UsageLogRepository usageLogRepository) {
        this.usageLogRepository = usageLogRepository;
    }

    /**
     * Creates and saves a new usage log.
     */
    public void recordUsage(
            Integer userId,
            Integer accessKeyId,
            Integer modelId,
            Integer promptTokens,
            Integer completionTokens,
            Integer imageCount,
            BigDecimal cost,
            LocalDateTime time,
            boolean isSuccess
    ) {
        UsageLogDocument log = new UsageLogDocument();
        log.setUserId(userId);
        log.setAccessKeyId(accessKeyId);
        log.setModelId(modelId);
        log.setPromptTokens(promptTokens);
        log.setCompletionTokens(completionTokens);
        log.setImageCount(imageCount);
        log.setCost(cost);
        log.setCreateTime(time);
        log.setIsSuccess(isSuccess);

        // Save the document to MongoDB. It's that simple!
        usageLogRepository.save(log);
    }

    /**
     * Finds logs for a specific user.
     */
    public List<UsageLogDocument> getLogsForUser(Integer userId) {
        return usageLogRepository.findByUserId(userId);
    }
    public List<UsageLogDocument> getLogsForModel(Integer modelId) {
        return usageLogRepository.findByModelId(modelId);
    }
}