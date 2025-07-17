package cn.tyt.llmproxy.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.dto.request.ModelCreateRequest;
import cn.tyt.llmproxy.dto.request.ModelStatusUpdateRequest;
import cn.tyt.llmproxy.dto.request.ModelUpdateRequest;
import cn.tyt.llmproxy.dto.response.ModelResponse;
import cn.tyt.llmproxy.service.ILlmModelService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/v1/models")
public class ModelController {

    @Autowired
    private ILlmModelService modelService;

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
}