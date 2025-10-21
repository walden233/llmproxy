package cn.tyt.llmproxy.listener;

import cn.tyt.llmproxy.config.RabbitMQConfig;
import cn.tyt.llmproxy.service.IOrderService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Component
public class OrderCancelListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCancelListener.class);

    @Autowired
    private IOrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCEL_QUEUE_NAME)
    @Transactional
    public void handleOrderCancellation(String orderNo, Message message, Channel channel) throws IOException {
        log.info("收到订单超时取消消息，订单号: {}", orderNo);
        try {
            // 1. 根据订单号查询订单
            orderService.cancelTtlOrder(orderNo);
            // 5. 手动确认消息已被成功处理
            log.info("订单 {} 已超时，自动取消成功。", orderNo);
        } catch (Exception e) {
            log.error("处理订单超时取消消息时发生错误，订单号: {}", orderNo, e);
            throw e;
            // 处理失败，可以选择将消息重新入队或拒绝（根据业务决定）
            // true: 重新入队，可能会导致死循环
            // false: 消息被丢弃或进入另一个死信队列（如果配置了）
        }
    }
}