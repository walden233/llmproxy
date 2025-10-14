package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.dto.request.KeyCreateRequest;
import cn.tyt.llmproxy.dto.request.KeyStatusUpdateRequest;
import cn.tyt.llmproxy.dto.request.ProviderCreateRequest;
import cn.tyt.llmproxy.dto.request.ProviderUpdateRequest;
import cn.tyt.llmproxy.entity.Provider;
import cn.tyt.llmproxy.entity.ProviderKey;
import cn.tyt.llmproxy.service.IProviderService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "供应商管理", description = "用于管理供应商及其API Keys")
@RestController
@RequestMapping("/v1/providers")
public class ProviderController {

    @Autowired
    private IProviderService providerService;

    @PostMapping
    @Operation(summary = "创建新的供应商")
    public Result<Provider> createProvider(@Valid @RequestBody ProviderCreateRequest request) {
        Provider provider = providerService.createProvider(request);
        return Result.success("供应商创建成功", provider);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取供应商信息")
    public Result<Provider> getProviderById(@PathVariable Integer id) {
        Provider provider = providerService.getProviderById(id);
        return Result.success(provider);
    }

    @GetMapping
    @Operation(summary = "分页获取所有供应商列表")
    public Result<IPage<Provider>> getAllProviders(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        IPage<Provider> page = providerService.getAllProviders(pageNum, pageSize);
        return Result.success(page);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新供应商信息")
    public Result<Provider> updateProvider(@PathVariable Integer id, @Valid @RequestBody ProviderUpdateRequest request) {
        Provider updatedProvider = providerService.updateProvider(id, request);
        return Result.success("供应商更新成功", updatedProvider);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除供应商及其所有关联的Key")
    public Result<?> deleteProvider(@PathVariable Integer id) {
        providerService.deleteProvider(id);
        return Result.success("供应商删除成功", null);
    }


    // --- 供应商密钥管理 (Provider Key Management) ---

    @PostMapping("/keys")
    @Operation(summary = "为供应商添加新的API Key")
    public Result<ProviderKey> addKeyToProvider(@Valid @RequestBody KeyCreateRequest request) {
        ProviderKey newKey = providerService.addKeyToProvider(request);
        return Result.success("API Key添加成功", newKey);
    }

    @GetMapping("/{providerId}/keys")
    @Operation(summary = "获取指定供应商的所有API Key")
    public Result<List<ProviderKey>> getKeysByProviderId(@PathVariable Integer providerId) {
        List<ProviderKey> keys = providerService.getKeysByProviderId(providerId);
        return Result.success(keys);
    }

    @PostMapping("/keys/{keyId}/status")
    @Operation(summary = "更新API Key的状态（激活/禁用）")
    public Result<ProviderKey> updateKeyStatus(@PathVariable Integer keyId, @Valid @RequestBody KeyStatusUpdateRequest request) {
        ProviderKey updatedKey = providerService.updateKeyStatus(keyId, request.getStatus());
        return Result.success("API Key状态更新成功", updatedKey);
    }

    @DeleteMapping("/keys/{keyId}")
    @Operation(summary = "删除指定的API Key")
    public Result<?> deleteKey(@PathVariable Integer keyId) {
        providerService.deleteKey(keyId);
        return Result.success("API Key删除成功", null);
    }
}