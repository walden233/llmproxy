package cn.tyt.llmproxy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.AcknowledgeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);
    /**
     * 定义一个 MessageConverter Bean，用于 RabbitMQ 的消息序列化和反序列化
     * 我们注入 Spring Boot 自动配置的 ObjectMapper，
     * 这样它就能使用 JacksonConfig 中定义的全局规则（如日期格式）
     *
     * @param objectMapper Spring Boot 自动注入的、已经由 JacksonConfig 定制过的 ObjectMapper
     * @return Jackson2JsonMessageConverter 实例
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // ========== 1. 业务交换机 ==========
    public static final String ORDER_EXCHANGE_NAME = "order.exchange";

    // ========== 2. 延迟队列 (用于存放新创建的订单消息) ==========
    public static final String ORDER_DELAY_QUEUE_NAME = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.create";

    // ========== 3. 死信交换机(当延迟队列消息过期后，会转发到这里) ==========
    public static final String ORDER_DLX_EXCHANGE_NAME = "order.dlx.exchange";

    // ========== 4. 死信队列 (真正的订单取消处理队列) ==========
    public static final String ORDER_CANCEL_QUEUE_NAME = "order.cancel.queue";
    public static final String ORDER_CANCEL_ROUTING_KEY = "order.cancel";
    public static final String ORDER_CANCEL_DLX_EXCHANGE_NAME = "order.cancel.dlx.exchange";
    public static final String ORDER_CANCEL_DLX_QUEUE_NAME = "order.cancel.dlq";
    public static final String ORDER_CANCEL_DLX_ROUTING_KEY = "order.cancel.dlx";

    // ========== 5. 异步任务相关配置 ==========
    public static final String ASYNC_TASK_EXCHANGE = "async.task.exchange";
    public static final String ASYNC_CHAT_QUEUE = "async.chat.queue";
    public static final String ASYNC_IMAGE_QUEUE = "async.image.queue";
    public static final String ASYNC_CHAT_ROUTING_KEY = "async.chat";
    public static final String ASYNC_IMAGE_ROUTING_KEY = "async.image";
    public static final String ASYNC_TASK_DLX_EXCHANGE = "async.task.dlx.exchange";
    public static final String ASYNC_CHAT_DLX_QUEUE = "async.chat.dlq";
    public static final String ASYNC_IMAGE_DLX_QUEUE = "async.image.dlq";
    public static final String ASYNC_CHAT_DLX_ROUTING_KEY = "async.chat.dlx";
    public static final String ASYNC_IMAGE_DLX_ROUTING_KEY = "async.image.dlx";

    //异步写mongodb
    public static final String STATS_EXCHANGE = "stats.exchange";
    public static final String STATS_QUEUE = "stats.usage.queue";
    public static final String STATS_ROUTING_KEY = "stats.usage";
    public static final String STATS_DLX_EXCHANGE = "stats.dlx.exchange";
    public static final String STATS_DLX_QUEUE = "stats.usage.dlq";
    public static final String STATS_DLX_ROUTING_KEY = "stats.usage.dlx";

    // 订单超时时间，单位：毫秒
    private static final long ORDER_TTL = 30*60*10000;

    /**
     * 1. 声明业务交换机(Direct 类型)
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
        // 设置消息的过期时间(TTL)，单位毫秒
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
     * 4. 声明死信交换机(Direct 类型)
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
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ORDER_CANCEL_DLX_EXCHANGE_NAME);
        args.put("x-dead-letter-routing-key", ORDER_CANCEL_DLX_ROUTING_KEY);
        return QueueBuilder.durable(ORDER_CANCEL_QUEUE_NAME).withArguments(args).build();
    }

    /**
     * 6. 将死信队列绑定到死信交换机
     */
    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue()).to(orderDlxExchange()).with(ORDER_CANCEL_ROUTING_KEY);
    }

    @Bean
    public DirectExchange orderCancelDlxExchange() {
        return new DirectExchange(ORDER_CANCEL_DLX_EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue orderCancelDlq() {
        return new Queue(ORDER_CANCEL_DLX_QUEUE_NAME, true);
    }

    @Bean
    public Binding orderCancelDlqBinding() {
        return BindingBuilder.bind(orderCancelDlq()).to(orderCancelDlxExchange()).with(ORDER_CANCEL_DLX_ROUTING_KEY);
    }

    /**
     * 7. 声明异步任务交换机
     */
    @Bean
    public DirectExchange asyncTaskExchange() {
        return new DirectExchange(ASYNC_TASK_EXCHANGE, true, false);
    }

    /**
     * 8. 声明异步聊天队列
     */
    @Bean
    public Queue asyncChatQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ASYNC_TASK_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", ASYNC_CHAT_DLX_ROUTING_KEY);
        return QueueBuilder.durable(ASYNC_CHAT_QUEUE).withArguments(args).build();
    }

    /**
     * 9. 声明异步图片生成队列
     */
    @Bean
    public Queue asyncImageQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ASYNC_TASK_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", ASYNC_IMAGE_DLX_ROUTING_KEY);
        return QueueBuilder.durable(ASYNC_IMAGE_QUEUE).withArguments(args).build();
    }

    /**
     * 10. 将异步聊天队列绑定到异步任务交换机
     */
    @Bean
    public Binding asyncChatBinding() {
        return BindingBuilder.bind(asyncChatQueue()).to(asyncTaskExchange()).with(ASYNC_CHAT_ROUTING_KEY);
    }

    /**
     * 11. 将异步图片生成队列绑定到异步任务交换机
     */
    @Bean
    public Binding asyncImageBinding() {
        return BindingBuilder.bind(asyncImageQueue()).to(asyncTaskExchange()).with(ASYNC_IMAGE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange statsExchange() {
        return new DirectExchange(STATS_EXCHANGE, true, false);
    }

    @Bean
    public Queue statsQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", STATS_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", STATS_DLX_ROUTING_KEY);
        return QueueBuilder.durable(STATS_QUEUE).withArguments(args).build();
    }

    @Bean
    public Binding statsBinding() {
        return BindingBuilder.bind(statsQueue()).to(statsExchange()).with(STATS_ROUTING_KEY);
    }

    @Bean
    public DirectExchange statsDlxExchange() {
        return new DirectExchange(STATS_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue statsDlq() {
        return new Queue(STATS_DLX_QUEUE, true);
    }

    @Bean
    public Binding statsDlqBinding() {
        return BindingBuilder.bind(statsDlq()).to(statsDlxExchange()).with(STATS_DLX_ROUTING_KEY);
    }

    @Bean
    public DirectExchange asyncTaskDlxExchange() {
        return new DirectExchange(ASYNC_TASK_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue asyncChatDlq() {
        return new Queue(ASYNC_CHAT_DLX_QUEUE, true);
    }

    @Bean
    public Queue asyncImageDlq() {
        return new Queue(ASYNC_IMAGE_DLX_QUEUE, true);
    }

    @Bean
    public Binding asyncChatDlqBinding() {
        return BindingBuilder.bind(asyncChatDlq()).to(asyncTaskDlxExchange()).with(ASYNC_CHAT_DLX_ROUTING_KEY);
    }

    @Bean
    public Binding asyncImageDlqBinding() {
        return BindingBuilder.bind(asyncImageDlq()).to(asyncTaskDlxExchange()).with(ASYNC_IMAGE_DLX_ROUTING_KEY);
    }

    //消费者端配置
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                               MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(10);
        return factory;
    }

    //生产者端配置
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.warn("RabbitMQ publish not confirmed, cause={}", cause);
            }
        });
        template.setReturnsCallback(returned -> log.warn(
                "RabbitMQ message returned: exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(), returned.getReplyText()
        ));
        return template;
    }
}
