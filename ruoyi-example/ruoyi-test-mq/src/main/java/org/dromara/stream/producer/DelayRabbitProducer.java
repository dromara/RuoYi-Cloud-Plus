package org.dromara.stream.producer;

import lombok.extern.slf4j.Slf4j;
import org.dromara.stream.config.RabbitTtlQueueConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author xbhog
 * @date 2024/05/25 17:15
 **/
@Slf4j
@Component
public class DelayRabbitProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendDelayMessage(String message, long delay) {
        rabbitTemplate.convertAndSend(
            RabbitTtlQueueConfig.DELAY_EXCHANGE_NAME,
            RabbitTtlQueueConfig.DELAY_ROUTING_KEY, message, message1 -> {
                message1.getMessageProperties().setDelayLong(delay);
                return message1;
            });
        log.info("【生产者】Delayed message send: " + message);
    }

}
