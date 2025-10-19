package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.dto.request.StatisticsQueryDto;
import cn.tyt.llmproxy.dto.response.ModelStatisticsDto;
import cn.tyt.llmproxy.entity.ModelDailyStat;
import cn.tyt.llmproxy.mapper.ModelDailyStatMapper;
import cn.tyt.llmproxy.service.IStatisticsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.dto.request.ModelCreateRequest;
import cn.tyt.llmproxy.dto.request.ModelStatusUpdateRequest;
import cn.tyt.llmproxy.dto.request.ModelUpdateRequest;
import cn.tyt.llmproxy.dto.response.ModelResponse;
import cn.tyt.llmproxy.service.ILlmModelService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/v1/models")
public class ModelController {

    @Autowired
    private ILlmModelService modelService;
    @Autowired
    private IStatisticsService statisticsService;

    @PostMapping
    public Result<ModelResponse> createModel(@Valid @RequestBody ModelCreateRequest request) {
        return Result.success(modelService.createModel(request));
    }

    @GetMapping("/{id}")
    public Result<ModelResponse> getModelById(@PathVariable Integer id) {
        return Result.success(modelService.getModelById(id));
    }

    @GetMapping
    public Result<IPage<ModelResponse>> getAllModels(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status, // "0" or "1"
            @RequestParam(required = false) String capability, // e.g., "text-to-text"
            @RequestParam(required = false, defaultValue = "priority") String sortBy, // e.g., "priority", "name", "createdAt"
            @RequestParam(required = false, defaultValue = "asc") String sortOrder // "asc" or "desc"
    ) {
        IPage<ModelResponse> page = modelService.getAllModels(pageNum, pageSize, status, capability, sortBy, sortOrder);
        return Result.success(page);
    }

    @PutMapping("/{id}")
    public Result<ModelResponse> updateModel(@PathVariable Integer id, @Valid @RequestBody ModelUpdateRequest request) {
        return Result.success(modelService.updateModel(id, request));
    }

    @PostMapping("/{id}/status")
    public Result<ModelResponse> updateModelStatus(@PathVariable Integer id, @Valid @RequestBody ModelStatusUpdateRequest request) {
        return Result.success(modelService.updateModelStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteModel(@PathVariable Integer id) {
        modelService.deleteModel(id);
        return Result.success("模型删除成功",null);
    }

    @PostMapping("/usage")
    //根据可选参数（模型ID、开始日期、结束日期）查询统计数据。如果所有参数都为空，则默认查询当天所有模型的统计。
    public Result<List<ModelStatisticsDto>> getModelStatistics(@Valid @RequestBody(required = false) StatisticsQueryDto queryDto) {
        // 如果请求体为空，则创建一个新的DTO对象，默认查询当天用量
        if (queryDto == null) {
            queryDto = new StatisticsQueryDto();
        }
        List<ModelStatisticsDto> result = statisticsService.getModelUsage(queryDto);
        return Result.success(result);
    }
}