package org.dromara.stream.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * @author xbhog
 * @date 2024/06/01 16:49
 **/
@Slf4j
@Component
public class NormalRocketProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void sendMessage() {
        SendResult sendResult = rocketMQTemplate.syncSend("TestTopic", MessageBuilder.withPayload("hello world test").build());
        log.info("发送普通同步消息-msg，syncSendMessage===>{}", sendResult);
    }
}
