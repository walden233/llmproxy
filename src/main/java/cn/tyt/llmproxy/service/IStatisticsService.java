package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.document.UsageLogDocument;
import cn.tyt.llmproxy.dto.request.StatisticsQueryDto;
import cn.tyt.llmproxy.dto.request.UsageLogQueryDto;
import cn.tyt.llmproxy.dto.response.ModelStatisticsDto;
import com.baomidou.mybatisplus.core.metadata.IPage;

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
     * 按条件查询模型日统计（列表，按日期升序返回）
     */
    List<ModelStatisticsDto> listModelDailyStats(StatisticsQueryDto queryDto);

    /**
     * 分页查询模型统计数据
     * @param queryDto 查询条件
     * @param pageNum 页码，从1开始
     * @param pageSize 每页条数
     * @return 分页统计结果
     */
    IPage<ModelStatisticsDto> pageModelDailyStats(StatisticsQueryDto queryDto, int pageNum, int pageSize);
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
