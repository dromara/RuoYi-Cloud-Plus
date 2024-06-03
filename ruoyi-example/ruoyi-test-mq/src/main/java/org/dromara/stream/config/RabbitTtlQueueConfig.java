package org.dromara.stream.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * RabbitTTL队列
 * @author xbhog
 */
@Configuration
public class RabbitTtlQueueConfig {

    // 延迟队列名称
    public static final String DELAY_QUEUE_NAME = "delay-queue";
    // 延迟交换机名称
    public static final String DELAY_EXCHANGE_NAME = "delay-exchange";
    // 延迟路由键名称
    public static final String DELAY_ROUTING_KEY = "delay.routing.key";

    // 死信交换机名称
    public static final String DEAD_LETTER_EXCHANGE = "dlx-exchange";
    // 死信队列名称
    public static final String DEAD_LETTER_QUEUE = "dlx-queue";
    // 死信路由键名称
    public static final String DEAD_LETTER_ROUTING_KEY = "dlx.routing.key";
    // 延迟消息的默认 TTL（毫秒）
    @Value("${rabbitmq.delay.ttl:5000}")
    private long messageTTL;

    // 声明延迟队列
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE_NAME)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY)
            .withArgument("x-message-ttl", messageTTL)
            .build();
    }
    // 声明延迟交换机
    @Bean
    public TopicExchange delayExchange() {
        return new TopicExchange(DELAY_EXCHANGE_NAME);
    }
    // 将延迟队列绑定到延迟交换机
    @Bean
    public Binding delayBinding(Queue delayQueue, TopicExchange delayExchange) {
        return BindingBuilder.bind(delayQueue).to(delayExchange).with(DELAY_ROUTING_KEY);
    }
    // 声明死信队列
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE);
    }
    // 声明死信交换机
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE);
    }

    // 将死信队列绑定到死信交换机
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_ROUTING_KEY);
    }
}

