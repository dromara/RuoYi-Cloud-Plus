package org.dromara.common.mqtt;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.dromara.common.mqtt.listener.MqttClientConnectListener;
import org.dromara.common.mqtt.listener.MqttClientGlobalMessageListener;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MqttListenerTest {
    @Test
    void connectionAndMessageCallbacksRecordLifecycleTopicAndDecodedPayload() {
        var connections = (Logger) LoggerFactory.getLogger(MqttClientConnectListener.class);
        var messages = (Logger) LoggerFactory.getLogger(MqttClientGlobalMessageListener.class);
        var logs = new ListAppender<ILoggingEvent>();
        logs.start();
        connections.addAppender(logs);
        messages.addAppender(logs);
        try {
            var listener = new MqttClientConnectListener(null);
            listener.onConnected(null, false);
            listener.onDisconnect(null, new IllegalStateException("connection closed"), "closed", true);
            new MqttClientGlobalMessageListener().onMessage(null, "events", null,
                "状态更新".getBytes(StandardCharsets.UTF_8));
            assertTrue(logs.list.get(0).getFormattedMessage().contains("MqttConnectedEvent"));
            assertTrue(logs.list.get(1).getFormattedMessage().contains("MqttDisconnectEvent"));
            assertTrue(logs.list.get(2).getFormattedMessage().contains("events"));
            assertTrue(logs.list.get(2).getFormattedMessage().contains("状态更新"));
        } finally {
            connections.detachAppender(logs);
            messages.detachAppender(logs);
            logs.stop();
        }
    }
}
