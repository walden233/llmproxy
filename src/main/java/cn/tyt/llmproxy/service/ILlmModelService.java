package cn.tyt.llmproxy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.tyt.llmproxy.dto.request.ModelCreateRequest;
import cn.tyt.llmproxy.dto.request.ModelStatusUpdateRequest;
import cn.tyt.llmproxy.dto.request.ModelUpdateRequest;
import cn.tyt.llmproxy.dto.response.ModelResponse;
import cn.tyt.llmproxy.entity.LlmModel;

public interface ILlmModelService {
    ModelResponse createModel(ModelCreateRequest request);
    ModelResponse getModelById(Integer id);
    IPage<ModelResponse> getAllModels(int pageNum, int pageSize, String status, String capability, String sortBy, String sortOrder);
    ModelResponse updateModel(Integer id, ModelUpdateRequest request);
    ModelResponse updateModelStatus(Integer id, ModelStatusUpdateRequest request);
    void deleteModel(Integer id);
}