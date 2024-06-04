package org.dromara.stream.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @author xbhog
 * @date 2024/06/01 16:53
 **/
@Slf4j
@Component
@RocketMQMessageListener(topic = "test-topic", consumerGroup = "test-consumer-group")
public class NormalRocketConsumer implements RocketMQListener<MessageExt> {

    @Override
    public void onMessage(MessageExt ext) {
        log.info("【消费者】接收消息：消息体 => {}, tag => {}", new String(ext.getBody()), ext.getTags());
    }

}
