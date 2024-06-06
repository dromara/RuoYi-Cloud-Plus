package org.dromara.stream.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
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
        // 发送普通消息
        // rocketMQTemplate.convertAndSend("test-topic", "test");

        // 发送带tag的消息
        Message<String> message = MessageBuilder.withPayload("test").setHeader(RocketMQHeaders.TAGS, "test-tag").build();
        rocketMQTemplate.send("test-topic", message);

        // 延迟消息
        // RocketMQ预定义了一些延迟等级，每个等级对应不同的延迟时间范围。这些等级从1到18，分别对应1s、5s、10s、30s、1m、2m、3m、4m、5m、6m、7m、8m、9m、10m、20m、30m、1h、2h的延迟时间。
        org.apache.rocketmq.common.message.Message msg = new org.apache.rocketmq.common.message.Message();
        msg.setDelayTimeLevel(3);
        try {
            rocketMQTemplate.getProducer().send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
