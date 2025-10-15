package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.dto.request.OrderCreateRequest;
import cn.tyt.llmproxy.dto.response.OrderResponse;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface IOrderService {

    /**
     * 生成新订单
     * @param request 订单创建请求
     * @return 创建的订单信息
     */
    OrderResponse generateOrder(OrderCreateRequest request);

    /**
     * 根据ID获取订单详情
     * @param id 订单ID
     * @return 订单详情
     */
    OrderResponse getOrderById(Integer id);
    OrderResponse getOrder(String orderNo);
    /**
     * 根据订单号处理支付成功回调
     * @param orderNo 订单号
     * @return 是否成功
     */
    OrderResponse paySuccess(String orderNo);

    /**
     * 根据订单号取消订单
     * @param orderNo 订单号
     * @return 更新后的订单信息
     */
    OrderResponse cancelOrder(String orderNo);

    /**
     * 根据查询条件分页获取订单列表
     * @param pageNum 当前页码
     * @param pageSize 每页数量
     * @param userId 用户ID (可选)
     * @param status 订单状态 (可选)
     * @return 分页的订单列表
     */
    IPage<OrderResponse> getAllOrders(int pageNum, int pageSize, Integer userId, String status);
}