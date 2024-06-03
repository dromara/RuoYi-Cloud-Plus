package org.dromara.stream.mq.producer.rabbitMq;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.stream.config.RabbitTtlQueueConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author xbhog
 * @date 2024/05/25 17:15
 **/
@Slf4j
@Component
public class DelayRabbitProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/sendDelay")
    public void sendDelayMessage(String message) {
        rabbitTemplate.convertAndSend(RabbitTtlQueueConfig.DELAY_EXCHANGE_NAME, RabbitTtlQueueConfig.DELAY_ROUTING_KEY, message);
        log.info("【生产者】Delayed message send: " + message);
    }
}
