package org.dromara.common.bus;

import org.dromara.common.bus.config.NoExternalStreamBinderCondition;
import org.dromara.common.bus.config.RedisBusProperties;
import org.dromara.common.bus.redis.RedisBusHealthIndicator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("common-bus 功能单元测试")
class BusFunctionTest {

    /**
     * 验证 Redis Bus 属性保存消费与死信配置，默认值覆盖认领、读取和容量上限。
     */
    @Test
    @DisplayName("保存 Redis Bus 属性")
    void shouldStoreRedisBusProperties() {
        RedisBusProperties properties = new RedisBusProperties();

        assertEquals(Duration.ofSeconds(30), properties.getClaimIdleTimeout());
        assertEquals(10, properties.getClaimCount());
        assertEquals(10, properties.getReadCount());
        assertEquals(Duration.ofSeconds(2), properties.getReadTimeout());
        assertEquals(3, properties.getMaxAttempts());
        assertTrue(properties.isDlqEnabled());
        assertEquals(10000, properties.getStreamMaxLen());

        properties.setClaimIdleTimeout(Duration.ofMinutes(1));
        properties.setClaimCount(5);
        properties.setReadCount(20);
        properties.setReadTimeout(Duration.ofSeconds(5));
        properties.setMaxAttempts(5);
        properties.setDlqEnabled(false);
        properties.setStreamMaxLen(5000);

        assertEquals(Duration.ofMinutes(1), properties.getClaimIdleTimeout());
        assertEquals(5, properties.getClaimCount());
        assertEquals(20, properties.getReadCount());
        assertEquals(Duration.ofSeconds(5), properties.getReadTimeout());
        assertEquals(5, properties.getMaxAttempts());
        assertFalse(properties.isDlqEnabled());
        assertEquals(5000, properties.getStreamMaxLen());
    }

    /**
     * 验证健康指示器探测 Redis 后报告存活，Redis 异常时报告不可用。
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldReportHealthByRedisConnectivity() {
        RedissonClient client = mock(RedissonClient.class);
        RBucket<Object> bucket = mock(RBucket.class);
        when(client.<Object>getBucket("spring:cloud:bus:health")).thenReturn(bucket);

        when(bucket.isExists()).thenReturn(true);
        Health up = new RedisBusHealthIndicator(client).health();
        assertEquals(Status.UP, up.getStatus());
        assertEquals("redis-stream", up.getDetails().get("binder"));

        when(bucket.isExists()).thenThrow(new RuntimeException("lost"));
        Health down = new RedisBusHealthIndicator(client).health();
        assertEquals(Status.DOWN, down.getStatus());
        assertEquals("redis-stream", down.getDetails().get("binder"));
    }

    /**
     * 验证无外部 Stream Binder 时条件成立，存在 Binder 或资源读取失败时条件不成立。
     */
    @Test
    @DisplayName("仅在无外部 Binder 时条件成立")
    void shouldMatchOnlyWithoutExternalBinder() throws IOException {
        NoExternalStreamBinderCondition condition = new NoExternalStreamBinderCondition();

        // classpath 无 META-INF/spring.binders 内容
        assertTrue(condition.matches(context(new Resource[]{
            new ByteArrayResource(new byte[0], "spring.binders")}), null));

        // 已存在外部 Binder（如 rabbit）
        assertFalse(condition.matches(context(new Resource[]{
            new ByteArrayResource("rabbit=\norg.springframework.cloud.stream.binder.rabbit.config.RabbitBinderConfiguration".getBytes(),
                "spring.binders")}), null));

        // 资源解析失败视为不匹配
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        ConditionContext failing = mock(ConditionContext.class);
        when(failing.getResourceLoader()).thenReturn(resolver);
        when(resolver.getResources("classpath*:META-INF/spring.binders")).thenThrow(new IOException("boom"));
        assertFalse(condition.matches(failing, null));
    }

    private static ConditionContext context(Resource[] resources) throws IOException {
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        ConditionContext context = mock(ConditionContext.class);
        when(context.getResourceLoader()).thenReturn(resolver);
        when(resolver.getResources("classpath*:META-INF/spring.binders")).thenReturn(resources);
        return context;
    }

}
