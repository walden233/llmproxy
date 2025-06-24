package cn.tyt.llmproxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.tyt.llmproxy.common.enums.ModelCapabilityEnum;
import cn.tyt.llmproxy.common.enums.StatusEnum;
import cn.tyt.llmproxy.dto.request.ModelCreateRequest;
import cn.tyt.llmproxy.dto.request.ModelStatusUpdateRequest;
import cn.tyt.llmproxy.dto.request.ModelUpdateRequest;
import cn.tyt.llmproxy.dto.response.ModelResponse;
import cn.tyt.llmproxy.entity.LlmModel;
import cn.tyt.llmproxy.mapper.LlmModelMapper;
import cn.tyt.llmproxy.service.ILlmModelService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LlmModelServiceImpl implements ILlmModelService {

    @Autowired
    private LlmModelMapper llmModelMapper;

    @Override
    @Transactional
    public ModelResponse createModel(ModelCreateRequest request) {
        // 校验 modelIdentifier 是否唯一
        if (llmModelMapper.selectOne(new LambdaQueryWrapper<LlmModel>()
                .eq(LlmModel::getModelIdentifier, request.getModelIdentifier())) != null) {
            throw new RuntimeException("模型标识 (Model Identifier) 已存在: " + request.getModelIdentifier());
        }

        // 校验 capabilities 是否合法
        if (request.getCapabilities() != null) {
            for (String cap : request.getCapabilities()) {
                if (!ModelCapabilityEnum.isValid(cap)) {
                    throw new RuntimeException("不支持的模型能力: " + cap);
                }
            }
        }

        LlmModel model = new LlmModel();
        BeanUtils.copyProperties(request, model); // 属性名一致的会自动拷贝
        model.setStatus(StatusEnum.AVAILABLE.getCode()); // 新建模型默认为上线状态
        model.setCreatedAt(LocalDateTime.now());
        model.setUpdatedAt(LocalDateTime.now());

        llmModelMapper.insert(model);
        return convertToResponse(model);
    }

    @Override
    public ModelResponse getModelById(Integer id) {
        LlmModel model = llmModelMapper.selectById(id);
        if (model == null) {
            throw new RuntimeException("模型未找到, ID: " + id);
        }
        return convertToResponse(model);
    }

    @Override
    public IPage<ModelResponse> getAllModels(int pageNum, int pageSize, String statusFilter, String capabilityFilter, String sortBy, String sortOrder) {
        Page<LlmModel> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LlmModel> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(statusFilter)) {
            try {
                int statusValue = Integer.parseInt(statusFilter);
                if (statusValue == StatusEnum.AVAILABLE.getCode() || statusValue == StatusEnum.UNAVAILABLE.getCode()) {
                    wrapper.eq(LlmModel::getStatus, statusValue);
                }
            } catch (NumberFormatException e) {
                // 忽略无效的状态筛选
            }
        }

        if (StringUtils.hasText(capabilityFilter)) {
            // 对于 JSON 数组的查询，MySQL 可以使用 JSON_CONTAINS
            // wrapper.apply("JSON_CONTAINS(capabilities, JSON_QUOTE({0}))", capabilityFilter);
            // 或者，如果只是简单匹配，可以在应用层过滤，或者查询出来后过滤
            // 这里为了简化，先查询所有，然后应用层过滤，或者如果量大，需要更复杂的SQL
            // MyBatis-Plus 不直接支持 JSON_CONTAINS，可能需要自定义 SQL 或在应用层过滤
            // 暂时不实现 capabilityFilter 的数据库层面过滤，可以在获取列表后用 Java Stream 过滤
        }

        // 排序
        if (StringUtils.hasText(sortBy)) {
            boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
            if ("priority".equalsIgnoreCase(sortBy)) {
                wrapper.orderBy(true, isAsc, LlmModel::getPriority);
            } else if ("name".equalsIgnoreCase(sortBy)) { // 假设按 displayName 排序
                wrapper.orderBy(true, isAsc, LlmModel::getDisplayName);
            } else if ("createdAt".equalsIgnoreCase(sortBy)) {
                wrapper.orderBy(true, isAsc, LlmModel::getCreatedAt);
            } else {
                wrapper.orderBy(true, true, LlmModel::getPriority); // 默认按优先级升序
            }
        } else {
            wrapper.orderByAsc(LlmModel::getPriority).orderByAsc(LlmModel::getDisplayName); // 默认排序
        }


        IPage<LlmModel> modelPage = llmModelMapper.selectPage(page, wrapper);

        // 转换 LlmModel 到 ModelResponse
        List<ModelResponse> responseList = modelPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 如果有 capabilityFilter，在这里进行应用层过滤
        if (StringUtils.hasText(capabilityFilter)) {
            String finalCapabilityFilter = capabilityFilter;
            responseList = responseList.stream()
                    .filter(mr -> mr.getCapabilities() != null && mr.getCapabilities().contains(finalCapabilityFilter))
                    .collect(Collectors.toList());
        }


        Page<ModelResponse> responsePage = new Page<>(modelPage.getCurrent(), modelPage.getSize(), modelPage.getTotal());
        responsePage.setRecords(responseList);
        return responsePage;
    }

    @Override
    @Transactional
    public ModelResponse updateModel(Integer id, ModelUpdateRequest request) {
        LlmModel existingModel = llmModelMapper.selectById(id);
        if (existingModel == null) {
            throw new RuntimeException("模型未找到, ID: " + id);
        }

        // 如果 modelIdentifier 被修改，需要检查唯一性
        if (StringUtils.hasText(request.getModelIdentifier()) &&
                !request.getModelIdentifier().equals(existingModel.getModelIdentifier())) {
            if (llmModelMapper.selectOne(new LambdaQueryWrapper<LlmModel>()
                    .eq(LlmModel::getModelIdentifier, request.getModelIdentifier())
                    .ne(LlmModel::getId, id)) != null) {
                throw new RuntimeException("模型标识 (Model Identifier) 已被其他模型使用: " + request.getModelIdentifier());
            }
            existingModel.setModelIdentifier(request.getModelIdentifier());
        }

        if (StringUtils.hasText(request.getDisplayName())) {
            existingModel.setDisplayName(request.getDisplayName());
        }
        if (StringUtils.hasText(request.getUrlBase())) {
            existingModel.setUrlBase(request.getUrlBase());
        }
        // API Key: 只有当请求中明确提供了 apiKey 时才更新
        if (StringUtils.hasText(request.getApiKey())) {
            existingModel.setApiKey(request.getApiKey());
        }
        if (request.getCapabilities() != null && !request.getCapabilities().isEmpty()) {
            for (String cap : request.getCapabilities()) {
                if (!ModelCapabilityEnum.isValid(cap)) {
                    throw new RuntimeException("不支持的模型能力: " + cap);
                }
            }
            existingModel.setCapabilities(request.getCapabilities());
        }
        if (request.getPriority() != null) {
            existingModel.setPriority(request.getPriority());
        }

        existingModel.setUpdatedAt(LocalDateTime.now());
        llmModelMapper.updateById(existingModel);
        return convertToResponse(existingModel);
    }

    @Override
    @Transactional
    public ModelResponse updateModelStatus(Integer id, ModelStatusUpdateRequest request) {
        LlmModel model = llmModelMapper.selectById(id);
        if (model == null) {
            throw new RuntimeException("模型未找到, ID: " + id);
        }
        if (request.getStatus() != StatusEnum.AVAILABLE.getCode() && request.getStatus() != StatusEnum.UNAVAILABLE.getCode()) {
            throw new RuntimeException("无效的状态值: " + request.getStatus());
        }
        model.setStatus(request.getStatus());
        model.setUpdatedAt(LocalDateTime.now());
        llmModelMapper.updateById(model);
        return convertToResponse(model);
    }

    @Override
    @Transactional
    public void deleteModel(Integer id) {
        LlmModel model = llmModelMapper.selectById(id);
        if (model == null) {
            throw new RuntimeException("模型未找到, ID: " + id + ",无法删除");
        }
        llmModelMapper.deleteById(id);
    }

    private ModelResponse convertToResponse(LlmModel model) {
        if (model == null) return null;
        ModelResponse response = new ModelResponse();
        BeanUtils.copyProperties(model, response);
        return response;
    }
}