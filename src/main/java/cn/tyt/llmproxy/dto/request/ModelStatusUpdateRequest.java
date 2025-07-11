package cn.tyt.llmproxy.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ModelStatusUpdateRequest {
    @NotNull(message = "状态不能为空")
    private Integer status; // 0: 下线, 1: 上线
}