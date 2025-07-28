package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.dto.request.StatisticsQueryDto;
import cn.tyt.llmproxy.dto.response.ModelStatisticsDto;

import java.time.LocalDate;
import java.util.List;

public interface IStatisticsService {
    /**
     * 记录一次模型调用
     * @param modelId 实际使用的模型ID
     * @param modelIdentifier 实际使用的模型标识
     * @param isSuccess 调用是否成功
     */
    void recordModelUsage(Integer modelId, String modelIdentifier, boolean isSuccess);
    /**
     * 根据灵活的查询条件获取模型统计数据
     * @param queryDto 包含 modelId, startDate, endDate 的查询对象
     * @return 统计数据列表
     */
    List<ModelStatisticsDto> getModelUsage(StatisticsQueryDto queryDto);
}