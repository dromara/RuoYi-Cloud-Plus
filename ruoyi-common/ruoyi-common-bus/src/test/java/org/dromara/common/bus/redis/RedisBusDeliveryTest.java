package org.dromara.common.bus.redis;

import org.dromara.common.bus.config.RedisBusProperties;
import org.dromara.common.bus.redis.RedisMessageChannelBinder.RedisBusMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.AutoClaimResult;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamAddParams;
import org.redisson.api.stream.StreamMessageId;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisBusDeliveryTest {

    private static final String STREAM = "spring:cloud:bus:events";
    private static final String GROUP = "events:system";
    private static final String CONSUMER = "events:node1:8080";

    private final RedissonClient redis = mock(RedissonClient.class);
    private final RStream<String, Object> stream = mock();
    private final RStream<String, Object> deadLetter = mock();
    private final RedisBusProperties properties = new RedisBusProperties();
    private final RedisMessageChannelBinder binder = new RedisMessageChannelBinder(redis, properties);

    RedisBusDeliveryTest() {
        var application = mock(GenericApplicationContext.class);
        var environment = mock(ConfigurableEnvironment.class);
        when(application.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("spring.cloud.bus.id")).thenReturn("system");
        when(environment.getProperty("spring.cloud.bus.redis.instance-id")).thenReturn("node1");
        when(environment.getProperty("server.port")).thenReturn("8080");
        binder.setApplicationContext(application);
        when(redis.<String, Object>getStream(STREAM)).thenReturn(stream);
        when(redis.<String, Object>getStream(STREAM + ":dlq:" + GROUP)).thenReturn(deadLetter);
    }

    @Test
    void producerStoresPayloadAndHeadersAndUnsubscribesOnUnbind() {
        var output = mock(SubscribableChannel.class);
        var binding = binder.doBindProducer("events", output, new ProducerProperties());
        var handler = org.mockito.ArgumentCaptor.forClass(MessageHandler.class);
        verify(output).subscribe(handler.capture());

        handler.getValue().handleMessage(MessageBuilder.withPayload("refresh").setHeader("origin", "system").build());

        verify(stream).add(argThat((StreamAddArgs<String, Object> args) -> {
            var stored = (StreamAddParams<String, Object>) args;
            var message = (RedisBusMessage) stored.getEntries().get("message");
            return "refresh".equals(message.getPayload()) && "system".equals(message.getHeaders().get("origin"))
                && stored.getMaxLen() == properties.getStreamMaxLen() && !stored.isTrimStrict();
        }));
        binding.unbind();
        verify(output).unsubscribe(handler.getValue());
    }

    @ParameterizedTest
    @CsvSource({"success,0,true", "retry,0,true", "exhausted,2,true", "discard,2,false"})
    void acknowledgesDeliveryAfterSuccessOrAfterHandlingFailure(String outcome, int previousAttempts,
                                                                boolean dlqEnabled) throws Exception {
        properties.setDlqEnabled(dlqEnabled);
        var id = new StreamMessageId(7, 1);
        var received = new RedisBusMessage("refresh", Map.of("destination", "system"));
        received.setAttempts(previousAttempts);
        var claimed = new AutoClaimResult<>(StreamMessageId.MIN, Map.of(id, Map.<String, Object>of("message", received)), List.of());
        var empty = new AutoClaimResult<String, Object>(StreamMessageId.MIN, Map.of(), List.of());
        when(stream.autoClaim(eq(GROUP), eq(CONSUMER), anyLong(), eq(TimeUnit.MILLISECONDS), eq(StreamMessageId.MIN), anyInt()))
            .thenReturn(claimed, empty);
        var acknowledged = new CountDownLatch(1);
        when(stream.ack(GROUP, id)).thenAnswer(call -> {
            acknowledged.countDown();
            return 1L;
        });
        when(stream.readGroup(eq(GROUP), eq(CONSUMER), any(org.redisson.api.stream.StreamReadGroupArgs.class)))
            .thenAnswer(call -> {
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                return Map.of();
            });
        var downstreamMessage = new AtomicReference<Message<?>>();
        MessageChannel input = (message, timeout) -> {
            downstreamMessage.set(message);
            if (!"success".equals(outcome)) {
                throw new IllegalStateException("delivery failed");
            }
            return true;
        };

        var binding = binder.doBindConsumer("events", "unused", input, new ConsumerProperties());
        try {
            assertTrue(acknowledged.await(5, TimeUnit.SECONDS), "consumer must acknowledge claimed message");
            assertEquals("refresh", downstreamMessage.get().getPayload());
            assertEquals("system", downstreamMessage.get().getHeaders().get("destination"));
            var ordered = inOrder(stream, deadLetter);
            if ("retry".equals(outcome)) {
                ordered.verify(stream).add(any(StreamAddArgs.class));
            } else if ("exhausted".equals(outcome)) {
                ordered.verify(deadLetter).add(any(StreamAddArgs.class));
            }
            ordered.verify(stream).ack(GROUP, id);
            verify(stream, times("retry".equals(outcome) ? 1 : 0)).add(any(StreamAddArgs.class));
            verify(deadLetter, times("exhausted".equals(outcome) ? 1 : 0)).add(any(StreamAddArgs.class));
            assertEquals("success".equals(outcome) ? previousAttempts : previousAttempts + 1, received.getAttempts());
        } finally {
            binding.unbind();
        }
        verify(stream).removeConsumer(GROUP, CONSUMER);
        assertFalse(binding.isRunning());
    }
}
