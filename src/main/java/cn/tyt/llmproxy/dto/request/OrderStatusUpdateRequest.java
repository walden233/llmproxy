package cn.tyt.llmproxy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {

    @NotBlank(message = "订单状态不能为空")
    private String status; // e.g., "COMPLETED", "FAILED"
}