package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.dto.request.StatisticsQueryDto;
import cn.tyt.llmproxy.dto.request.UsageLogQueryDto;
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
@PreAuthorize("hasAnyAuthority('ROLE_ROOT_ADMIN','ROLE_MODEL_ADMIN')")
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
    @PostMapping("/models")
    public Result<List<ModelStatisticsDto>> getModelDailyStats(
            @Valid @RequestBody(required = false) StatisticsQueryDto queryDto) {
        // 如果请求体为空，则创建一个新的DTO对象，以触发默认查询逻辑（例如查询当天）
        if (queryDto == null) {
            queryDto = new StatisticsQueryDto();
        }
        List<ModelStatisticsDto> result = statisticsService.listModelDailyStats(queryDto);
        return Result.success(result);
    }

    /**
     * 查询指定用户的详细用量日志
     * @param userId 用户ID
     * @return 用户的用量日志列表
     */
    @GetMapping("/logs/user/{userId}")
    public Result<List<UsageLogDocument>> getUserLogs(@PathVariable Integer userId) {
        List<UsageLogDocument> logs = statisticsService.getLogsForUser(userId);
        return Result.success(logs);
    }

    @GetMapping("/logs/me")
    @PreAuthorize("isAuthenticated()")
    public Result<List<UsageLogDocument>> getMyLogs() {
        int userId = userService.getCurrentUser().getId();
        List<UsageLogDocument> logs = statisticsService.getLogsForUser(userId);
        return Result.success(logs);
    }

    /**
     * 查询指定模型的详细用量日志
     * @param modelId 模型ID
     * @return 模型的用量日志列表
     */
    @GetMapping("/logs/model/{modelId}")
    public Result<List<UsageLogDocument>> getModelLogs(@PathVariable Integer modelId) {
        List<UsageLogDocument> logs = statisticsService.getLogsForModel(modelId);
        return Result.success(logs);
    }

    /**
     * 根据自定义条件筛选用量日志（支持用户/模型/时间/成功状态等组合条件）。
     */
    @PostMapping("/logs/query")
    public Result<List<UsageLogDocument>> queryUsageLogs(@RequestBody(required = false) UsageLogQueryDto queryDto) {
        List<UsageLogDocument> logs = statisticsService.queryUsageLogs(queryDto);
        return Result.success(logs);
    }
}
