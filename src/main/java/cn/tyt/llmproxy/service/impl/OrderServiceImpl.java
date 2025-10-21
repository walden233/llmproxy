package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.config.RabbitMQConfig;
import cn.tyt.llmproxy.dto.request.OrderCreateRequest;
import cn.tyt.llmproxy.dto.response.OrderResponse;
import cn.tyt.llmproxy.entity.Order;
import cn.tyt.llmproxy.mapper.OrderMapper;
import cn.tyt.llmproxy.service.IOrderService;
import cn.tyt.llmproxy.service.IUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class OrderServiceImpl implements IOrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private IUserService userService; // 注入用户服务
    @Autowired
    private RabbitTemplate rabbitTemplate; // 注入RabbitTemplate

    @Override
    @Transactional
    //使用rabbitMQ定时取消订单
    public OrderResponse generateOrder(OrderCreateRequest request) {
        Order order = new Order();
        BeanUtils.copyProperties(request, order);

        // 生成唯一的订单号
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setStatus(Order.STATUS_PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        orderMapper.insert(order);

        // 2. 发送延迟消息到RabbitMQ
        try {
            String orderNo = order.getOrderNo();
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE_NAME,
                    RabbitMQConfig.ORDER_DELAY_ROUTING_KEY,
                    orderNo // 消息内容就是订单号
            );
            log.info("已发送订单超时检查消息，订单号: {}", orderNo);
        } catch (Exception e) {
            // 消息发送失败可以记录日志，或者进行异常处理
            log.error("发送订单超时检查消息失败，订单号: {}，错误: {}", order.getOrderNo(), e.getMessage());
        }

        return convertToResponse(order);
    }

    @Override
    public OrderResponse getOrderById(Integer id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            // 在实际项目中，最好抛出一个自定义的资源未找到异常
            throw new RuntimeException("Order not found with id: " + id);
        }
        return convertToResponse(order);
    }

    @Override
    public OrderResponse getOrder(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo,orderNo));
        if (order == null) {
            throw new RuntimeException("Order not found with orderNo: " + orderNo);
        }
        return convertToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse paySuccess(String orderNo) { // 建议返回 OrderResponse
        Order order = findOrderByOrderNo(orderNo);

        // 1. 幂等性检查：如果订单状态不是 PENDING，直接返回当前状态，表示已处理过
        if (!Order.STATUS_PENDING.equals(order.getStatus())) {
            // 对于已经完成的订单，可以直接返回成功，让客户端重试逻辑更简单
            if (Order.STATUS_COMPLETED.equals(order.getStatus())) {
                return convertToResponse(order);
            }
            // 对于其他状态，则抛出异常
            throw new IllegalStateException("Order status is not PENDING, cannot process payment.");
        }

        // 2. 更新订单状态
        order.setStatus(Order.STATUS_COMPLETED);
        order.setUpdatedAt(LocalDateTime.now());

        // 3. 利用乐观锁进行更新
        int updatedRows = orderMapper.updateById(order);
        if (updatedRows == 0) {
            // 如果更新影响的行数为0，说明版本号已变，有并发冲突
            throw new RuntimeException("Order status update failed due to a concurrency conflict.");
        }

        // 4. 调用用户服务增加余额 (关注点分离)
        // 传入订单金额和用户ID
        try {
            userService.creditUserBalance(order.getUserId(), order.getAmount());
        } catch (Exception e) {
            // 如果用户服务失败，也需要抛出异常，让整个事务回滚
            throw new RuntimeException("Failed to credit user balance for order: " + orderNo, e);
        }

        return convertToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String orderNo) {
        Order order = findOrderByOrderNo(orderNo);
        // 只有待支付的订单才能被取消
        if (Order.STATUS_COMPLETED.equals(order.getStatus())) {
            throw new IllegalStateException("Only PENDING orders can be cancelled.");
        }
        if (Order.STATUS_FAILED.equals(order.getStatus())) {
            return convertToResponse(order);
        }
        order.setStatus(Order.STATUS_FAILED); // 或自定义一个 STATUS_CANCELLED
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return convertToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelTtlOrder(String orderNo) {
        Order order = findOrderByOrderNo(orderNo);
        // 由于用于自动取消，状态不对不报错
        if (!Order.STATUS_PENDING.equals(order.getStatus())) {
            return convertToResponse(order);
        }
        order.setStatus(Order.STATUS_FAILED);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return convertToResponse(order);
    }

    @Override
    public IPage<OrderResponse> getAllOrders(int pageNum, int pageSize, Integer userId, String status) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();

        // 添加查询条件
        queryWrapper.eq(Objects.nonNull(userId), Order::getUserId, userId);
        queryWrapper.eq(StringUtils.hasText(status), Order::getStatus, status);

        // 默认按创建时间降序排序
        queryWrapper.orderByDesc(Order::getCreatedAt);

        IPage<Order> orderPage = orderMapper.selectPage(page, queryWrapper);

        // 将 IPage<Order> 转换为 IPage<OrderResponse>
        return orderPage.convert(this::convertToResponse);
    }

    private Order findOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderMapper.selectOne(queryWrapper);
        if (order == null) {
            throw new RuntimeException("Order not found with orderNo: " + orderNo);
        }
        return order;
    }

    private OrderResponse convertToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        BeanUtils.copyProperties(order, response);
        return response;
    }
}