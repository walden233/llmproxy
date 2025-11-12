package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.dto.UsageLogDocument;
import cn.tyt.llmproxy.dto.request.StatisticsQueryDto;
import cn.tyt.llmproxy.dto.request.UsageLogQueryDto;
import cn.tyt.llmproxy.dto.response.ModelStatisticsDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IStatisticsService {
    /**
     * 记录一次模型调用
     * @param modelId 实际使用的模型ID
     * @param modelIdentifier 实际使用的模型标识
     * @param isSuccess 调用是否成功
     */
    void recordUsageMysql(Integer modelId, String modelIdentifier, boolean isSuccess);
    /**
     * 根据灵活的查询条件获取模型统计数据
     * @param queryDto 包含 modelId, startDate, endDate 的查询对象
     * @return 统计数据列表
     */
    List<ModelStatisticsDto> getModelUsage(StatisticsQueryDto queryDto);
    public void recordUsageMongo(
            Integer userId,
            Integer accessKeyId,
            Integer modelId,
            Integer promptTokens,
            Integer completionTokens,
            Integer imageCount,
            BigDecimal cost,
            LocalDateTime time,
            boolean isSuccess,
            Boolean isAsync
    );
    public void recordFailMongo(
            Integer userId,
            Integer accessKeyId,
            Integer modelId,
            LocalDateTime time,
            Boolean isAsync
    );

    public List<UsageLogDocument> getLogsForUser(Integer userId);
    public List<UsageLogDocument> getLogsForModel(Integer modelId);
    /**
     * 按自定义条件检索 Mongo 用量日志。
     */
    List<UsageLogDocument> queryUsageLogs(UsageLogQueryDto queryDto);
}
