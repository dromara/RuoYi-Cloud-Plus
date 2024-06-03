package org.dromara.stream.producer;

import lombok.extern.slf4j.Slf4j;
import org.dromara.stream.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author xbhog
 */
@Slf4j
@Component
public class NormalRabbitProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(String message) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.ROUTING_KEY, message);
        log.info("【生产者】Message send: " + message);
    }
}
