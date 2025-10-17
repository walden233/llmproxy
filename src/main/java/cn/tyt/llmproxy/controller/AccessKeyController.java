package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.entity.AccessKey;
import cn.tyt.llmproxy.service.IAccessKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/access-keys")
public class AccessKeyController {

    @Autowired
    private IAccessKeyService accessKeyService;


    /**
     * 为当前登录的用户创建一个新的Access Key
     */
    @PostMapping
    public Result<AccessKey> createAccessKey() {
        AccessKey accessKey = accessKeyService.createAccessKey();
        return Result.success(accessKey);
    }

    /**
     * 获取当前用户的所有Access Key
     */
    @GetMapping
    public Result<List<AccessKey>> listAccessKeys() {
        List<AccessKey> keys = accessKeyService.getAccessKeys();
        return Result.success(keys);
    }

    /**
     * 删除当前用户的一个指定Access Key
     * @param id 要删除的Access Key的ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAccessKey(@PathVariable Integer id) {
        accessKeyService.deleteMyAccessKey(id);
        return Result.success(); // 删除成功，返回一个没有数据体的成功响应
    }

}
