package org.dromara.stream.mq.producer.rocketMq;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * @author xbhog
 * @date 2024/06/01 16:49
 **/
@Slf4j
@Component
public class NormalRocketProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void sendMessage(){
        SendResult sendResult = rocketMQTemplate.syncSend("TestTopic", MessageBuilder.withPayload("hello world test").build());
        log.info("发送普通同步消息-msg，syncSendMessage===>{}", sendResult);
    }
}
