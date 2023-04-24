package org.dromara.stream.mq.producer;

import org.dromara.stream.mq.TestMessaging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LogStreamProducer {

    @Autowired
    private StreamBridge streamBridge;

    public void streamLogMsg(String msg) {
        // 构建消息对象
        TestMessaging testMessaging = new TestMessaging()
            .setMsgId(UUID.randomUUID().toString())
            .setMsgText(msg);
        streamBridge.send("log-out-0", MessageBuilder.withPayload(testMessaging).build());
    }
}
