package cn.tyt.llmproxy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.tyt.llmproxy.dto.request.KeyCreateRequest;
import cn.tyt.llmproxy.dto.request.ProviderCreateRequest;
import cn.tyt.llmproxy.dto.request.ProviderUpdateRequest;
import cn.tyt.llmproxy.entity.Provider;
import cn.tyt.llmproxy.entity.ProviderKey;

import java.util.List;

public interface IProviderService {

    // --- Provider 管理 ---
    Provider createProvider(ProviderCreateRequest request);
    Provider getProviderById(Integer providerId);
    IPage<Provider> getAllProviders(int pageNum, int pageSize);
    Provider updateProvider(Integer providerId, ProviderUpdateRequest request);
    void deleteProvider(Integer providerId);

    // --- ProviderKey 管理 ---
    ProviderKey addKeyToProvider(KeyCreateRequest request);
    List<ProviderKey> getKeysByProviderId(Integer providerId);
    ProviderKey updateKeyStatus(Integer keyId, Boolean status);
    void deleteKey(Integer keyId);
}