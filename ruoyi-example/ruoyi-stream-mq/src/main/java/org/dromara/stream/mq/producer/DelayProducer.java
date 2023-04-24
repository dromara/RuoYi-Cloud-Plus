package org.dromara.stream.mq.producer;

import org.dromara.stream.mq.TestMessaging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DelayProducer {

    @Autowired
    private StreamBridge streamBridge;

    public void sendMsg(String msg, Long delay) {
        // 构建消息对象
        TestMessaging testMessaging = new TestMessaging()
            .setMsgId(UUID.randomUUID().toString())
            .setMsgText(msg);
        Message<TestMessaging> message = MessageBuilder.withPayload(testMessaging)
            .setHeader("x-delay", delay).build();
        streamBridge.send("delay-out-0", message);
    }
}
