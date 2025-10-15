package cn.tyt.llmproxy.controller;

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.dto.request.OrderCreateRequest;
import cn.tyt.llmproxy.dto.response.OrderNoResponse;
import cn.tyt.llmproxy.dto.response.OrderResponse;
import cn.tyt.llmproxy.service.IOrderService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    @Autowired
    private IOrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping
    public Result<OrderResponse> generateOrder(@Valid @RequestBody OrderCreateRequest request) {
        return Result.success(orderService.generateOrder(request));
    }

    /**
     * 根据orderNo获取订单
     */
    @GetMapping("/get")
    public Result<OrderResponse> getOrder(@RequestParam String orderNo) {
        return Result.success(orderService.getOrder(orderNo));
    }

    /**
     * 获取订单列表（分页和条件查询）
     */
    @GetMapping
    public Result<IPage<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String status) {
        return Result.success(orderService.getAllOrders(pageNum, pageSize, userId, status));
    }

    /**
     * 支付成功回调接口
     */
    @PostMapping("/pay-success")
    public Result<OrderResponse> paySuccessCallback(@RequestBody OrderNoResponse orderNo) {
        return Result.success(orderService.paySuccess(orderNo.getOrderNo()));
    }

    /**
     * 取消订单接口
     */
    @PostMapping("/cancel")
    public Result<OrderResponse> cancelOrder(@RequestBody OrderNoResponse orderNo) {
        return Result.success(orderService.cancelOrder(orderNo.getOrderNo()));
    }
}
