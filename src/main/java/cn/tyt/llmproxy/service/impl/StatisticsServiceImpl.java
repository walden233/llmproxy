package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.dto.request.StatisticsQueryDto;
import cn.tyt.llmproxy.dto.response.ModelStatisticsDto;
import cn.tyt.llmproxy.entity.ModelDailyStat;
import cn.tyt.llmproxy.mapper.ModelDailyStatMapper;
import cn.tyt.llmproxy.service.IStatisticsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements IStatisticsService {

    private final ModelDailyStatMapper modelDailyStatMapper;

    @Override
    public void recordModelUsage(Integer modelId, String modelIdentifier, boolean isSuccess) {
        ModelDailyStat stat = ModelDailyStat.builder()
                .modelId(modelId)
                .modelIdentifier(modelIdentifier)
                .statDate(LocalDate.now()) // 统计日期为当天
                .totalRequests(1) // 每次调用总请求数+1
                .successCount(isSuccess ? 1 : 0) // 成功则成功数+1
                .failureCount(isSuccess ? 0 : 1) // 失败则失败数+1
                .build();

        modelDailyStatMapper.upsert(stat);
    }


    @Override
    public List<ModelStatisticsDto> getModelUsage(StatisticsQueryDto queryDto) {
        LambdaQueryWrapper<ModelDailyStat> queryWrapper = new LambdaQueryWrapper<>();

        // 1. 处理 modelId
        if (queryDto.getModelId() != null) {
            queryWrapper.eq(ModelDailyStat::getModelId, queryDto.getModelId());
        } else if(queryDto.getModelIdentifier() != null){
            queryWrapper.eq(ModelDailyStat::getModelIdentifier, queryDto.getModelIdentifier());
        }

        // 2. 处理日期
        LocalDate date = queryDto.getDate();

        if (date == null) {
            // 如果都为空，查询当天
            queryWrapper.eq(ModelDailyStat::getStatDate, LocalDate.now());
        } else{
            queryWrapper.eq(ModelDailyStat::getStatDate, date);
        }

        // 排序，让结果更可读
        queryWrapper.orderByAsc(ModelDailyStat::getModelId, ModelDailyStat::getStatDate);
        List<ModelDailyStat> stats = modelDailyStatMapper.selectList(queryWrapper);
        return convertToDto(stats);
    }

    private List<ModelStatisticsDto> convertToDto(List<ModelDailyStat> stats) {
            return stats.stream().map(stat -> {
                ModelStatisticsDto dto = new ModelStatisticsDto();
                // 可以在这里添加 modelId 和 modelIdentifier
                dto.setModelId(stat.getModelId());
                dto.setModelIdentifier(stat.getModelIdentifier());
                dto.setDate(stat.getStatDate());
                dto.setTotalRequests(stat.getTotalRequests());
                dto.setSuccessCount(stat.getSuccessCount());
                dto.setFailureCount(stat.getFailureCount());
                return dto;
            }).collect(Collectors.toList());
    }
}
