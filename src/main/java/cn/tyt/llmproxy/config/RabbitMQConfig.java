package cn.tyt.llmproxy.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // ========== 1. 业务交换机 ==========
    public static final String ORDER_EXCHANGE_NAME = "order.exchange";

    // ========== 2. 延迟队列 (用于存放新创建的订单消息) ==========
    public static final String ORDER_DELAY_QUEUE_NAME = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.create";

    // ========== 3. 死信交换机 (当延迟队列消息过期后，会转发到这里) ==========
    public static final String ORDER_DLX_EXCHANGE_NAME = "order.dlx.exchange";

    // ========== 4. 死信队列 (真正的订单取消处理队列) ==========
    public static final String ORDER_CANCEL_QUEUE_NAME = "order.cancel.queue";
    public static final String ORDER_CANCEL_ROUTING_KEY = "order.cancel";

    // 订单超时时间，单位：毫秒
    private static final long ORDER_TTL = 30*60*10000;

    /**
     * 1. 声明业务交换机 (Direct 类型)
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE_NAME, true, false);
    }

    /**
     * 2. 声明延迟队列，并绑定死信交换机
     * 当这个队列的消息过期后，会自动投递到 x-dead-letter-exchange 指定的交换机
     */
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        // 设置消息的过期时间 (TTL)，单位毫秒
        args.put("x-message-ttl", ORDER_TTL);
        // 绑定死信交换机
        args.put("x-dead-letter-exchange", ORDER_DLX_EXCHANGE_NAME);
        // 指定死信消息的路由键
        args.put("x-dead-letter-routing-key", ORDER_CANCEL_ROUTING_KEY);

        return QueueBuilder.durable(ORDER_DELAY_QUEUE_NAME)
                .withArguments(args)
                .build();
    }

    /**
     * 3. 将延迟队列绑定到业务交换机
     */
    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderExchange()).with(ORDER_DELAY_ROUTING_KEY);
    }

    /**
     * 4. 声明死信交换机 (Direct 类型)
     */
    @Bean
    public DirectExchange orderDlxExchange() {
        return new DirectExchange(ORDER_DLX_EXCHANGE_NAME, true, false);
    }

    /**
     * 5. 声明死信队列 (用于处理超时的订单)
     */
    @Bean
    public Queue orderCancelQueue() {
        return new Queue(ORDER_CANCEL_QUEUE_NAME, true);
    }

    /**
     * 6. 将死信队列绑定到死信交换机
     */
    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue()).to(orderDlxExchange()).with(ORDER_CANCEL_ROUTING_KEY);
    }
}