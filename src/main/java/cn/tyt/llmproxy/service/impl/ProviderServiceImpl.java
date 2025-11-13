package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.common.enums.ResultCode;
import cn.tyt.llmproxy.common.exception.BusinessException;
import cn.tyt.llmproxy.dto.request.KeyCreateRequest;
import cn.tyt.llmproxy.dto.request.ProviderCreateRequest;
import cn.tyt.llmproxy.dto.request.ProviderUpdateRequest;
import cn.tyt.llmproxy.entity.LlmModel;
import cn.tyt.llmproxy.entity.Provider;
import cn.tyt.llmproxy.entity.ProviderKey;
import cn.tyt.llmproxy.mapper.LlmModelMapper;
import cn.tyt.llmproxy.mapper.ProviderKeyMapper;
import cn.tyt.llmproxy.mapper.ProviderMapper;
import cn.tyt.llmproxy.service.IProviderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProviderServiceImpl implements IProviderService {

    @Autowired
    private ProviderMapper providerMapper;
    @Autowired
    private ProviderKeyMapper providerKeyMapper;
    @Autowired
    private LlmModelMapper llmModelMapper; // 用于检查 Provider 是否被模型关联

    // --- Provider 实现 ---

    @Override
    @Transactional
    public Provider createProvider(ProviderCreateRequest request) {
        if (providerMapper.selectOne(new LambdaQueryWrapper<Provider>().eq(Provider::getName, request.getName())) != null) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "供应商名称已存在: " + request.getName());
        }
        Provider provider = new Provider();
        BeanUtils.copyProperties(request, provider);
        providerMapper.insert(provider);
        return provider;
    }

    @Override
    public Provider getProviderById(Integer providerId) {
        Provider provider = providerMapper.selectById(providerId);
        if (provider == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "未找到指定的供应商, ID: " + providerId);
        }
        return provider;
    }

    @Override
    public IPage<Provider> getAllProviders(int pageNum, int pageSize) {
        return providerMapper.selectPage(new Page<>(pageNum, pageSize), null);
    }

    @Override
    @Transactional
    public Provider updateProvider(Integer providerId, ProviderUpdateRequest request) {
        Provider existingProvider = getProviderById(providerId); // 复用查询和非空检查

        // 如果名称被修改，检查新名称是否唯一
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(existingProvider.getName())) {
            if (providerMapper.selectOne(new LambdaQueryWrapper<Provider>().eq(Provider::getName, request.getName())) != null) {
                throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "供应商名称已被占用: " + request.getName());
            }
        }

        BeanUtils.copyProperties(request, existingProvider);
        existingProvider.setUpdatedAt(LocalDateTime.now());
        providerMapper.updateById(existingProvider);
        return existingProvider;
    }

    @Override
    @Transactional
    public void deleteProvider(Integer providerId) {
        getProviderById(providerId); // 检查是否存在

        // 检查是否有模型正在使用此 Provider
        Long modelCount = llmModelMapper.selectCount(new LambdaQueryWrapper<LlmModel>().eq(LlmModel::getProviderId, providerId));
        if (modelCount > 0) {
            throw new BusinessException(ResultCode.ILLEGAL_STATE, "无法删除！有 " + modelCount + " 个模型正在使用此供应商。");
        }

        // 1. 删除所有关联的 Key
        providerKeyMapper.delete(new LambdaQueryWrapper<ProviderKey>().eq(ProviderKey::getProviderId, providerId));

        // 2. 删除 Provider 自身
        providerMapper.deleteById(providerId);
    }


    // --- ProviderKey 实现 ---

    @Override
    @Transactional
    public ProviderKey addKeyToProvider(KeyCreateRequest request) {
        // 确保关联的 Provider 存在
        ProviderKey key = new ProviderKey();
        if(request.getProviderId()!=null){
            getProviderById(request.getProviderId());
            key.setProviderId(request.getProviderId());
            key.setApiKey(request.getApiKey());
            key.setStatus(ProviderKey.STATUS_ACTIVE); // 默认激活
        }
        else if(StringUtils.hasText(request.getProviderName())){
            Provider provider = providerMapper.selectOne(new LambdaQueryWrapper<Provider>()
                    .eq(Provider::getName, request.getProviderName()));
            if(provider==null)
                throw new BusinessException(ResultCode.DATA_NOT_FOUND, "未找到指定的供应商: " + request.getProviderName());
            key.setProviderId(provider.getId());
            key.setApiKey(request.getApiKey());
            key.setStatus(ProviderKey.STATUS_ACTIVE);
        }
        else
            throw new BusinessException(ResultCode.PARAM_MISSING, "未提供供应商名称或id");
        providerKeyMapper.insert(key);
        return key;
    }

    @Override
    public List<ProviderKey> getKeysByProviderId(Integer providerId) {
        getProviderById(providerId); // 确保 Provider 存在
        return providerKeyMapper.selectList(new LambdaQueryWrapper<ProviderKey>().eq(ProviderKey::getProviderId, providerId));
    }

    @Override
    @Transactional
    public ProviderKey updateKeyStatus(Integer keyId, Boolean status) {
        ProviderKey key = providerKeyMapper.selectById(keyId);
        if (key == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "未找到指定的 API Key, ID: " + keyId);
        }
        key.setStatus(status?1:0);
        key.setUpdatedAt(LocalDateTime.now());
        providerKeyMapper.updateById(key);
        return key;
    }

    @Override
    @Transactional
    public void deleteKey(Integer keyId) {
        int deletedRows = providerKeyMapper.deleteById(keyId);
        if (deletedRows == 0) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "未找到要删除的 API Key, ID: " + keyId);
        }
    }
}
