package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.dto.request.StatisticsQueryDto;
import cn.tyt.llmproxy.dto.response.ModelStatisticsDto;
import cn.tyt.llmproxy.dto.UsageLogDocument;
import cn.tyt.llmproxy.service.IStatisticsService;
import cn.tyt.llmproxy.service.IUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 统计和日志相关的API接口
 */
@RestController
@RequestMapping("/v1/statistics")
@PreAuthorize("hasAnyAuthority('root_admin','model_admin')")
@Tag(name = "Statistics Management", description = "用于管理和查询使用统计与日志的API")
public class StatisticsController {

    @Autowired
    private IStatisticsService statisticsService;
    @Autowired
    private IUserService userService;

    /**
     * 根据可选参数（模型ID、开始日期、结束日期）查询模型使用统计数据。
     * 如果所有参数都为空，则默认查询当天所有模型的统计。
     * @param queryDto 查询参数 DTO
     * @return 模型统计数据列表
     */
    @PostMapping
    public Result<List<ModelStatisticsDto>> getModelStatistics(@Valid @RequestBody(required = false) StatisticsQueryDto queryDto) {
        // 如果请求体为空，则创建一个新的DTO对象，以触发默认查询逻辑（例如查询当天）
        if (queryDto == null) {
            queryDto = new StatisticsQueryDto();
        }
        List<ModelStatisticsDto> result = statisticsService.getModelUsage(queryDto);
        return Result.success(result);
    }

    /**
     * 查询指定用户的详细用量日志
     * @param userId 用户ID
     * @return 用户的用量日志列表
     */
    @GetMapping("/user/{userId}")
    public Result<List<UsageLogDocument>> getLogsForUser(@PathVariable Integer userId) {
        List<UsageLogDocument> logs = statisticsService.getLogsForUser(userId);
        return Result.success(logs);
    }

    @GetMapping("/mylog")
    @PreAuthorize("isAuthenticated()")
    public Result<List<UsageLogDocument>> getLogsForCurrentUser() {
        int userId = userService.getCurrentUser().getId();
        List<UsageLogDocument> logs = statisticsService.getLogsForUser(userId);
        return Result.success(logs);
    }

    /**
     * 查询指定模型的详细用量日志
     * @param modelId 模型ID
     * @return 模型的用量日志列表
     */
    @GetMapping("/model/{modelId}")
    public Result<List<UsageLogDocument>> getLogsForModel(@PathVariable Integer modelId) {
        List<UsageLogDocument> logs = statisticsService.getLogsForModel(modelId);
        return Result.success(logs);
    }
}