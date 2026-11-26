package org.apache.dubbo.metadata.store.redis;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedissonMetadataReportTest {

    private final URL reportUrl = URL.valueOf("redis://localhost:6379?cycle-report=false");
    private final RedissonMetadataReport report = new RedissonMetadataReport(reportUrl);
    private final RedissonClient redis = mock(RedissonClient.class);

    RedissonMetadataReportTest() {
        ReflectionTestUtils.setField(report, "redissonClient", redis);
    }

    @AfterEach
    void destroyReport() {
        report.destroy();
    }

    @Test
    void roundTripsEncodedServiceUrlAndRemovesItsMetadata() {
        var id = mock(ServiceMetadataIdentifier.class);
        when(id.getIdentifierKey()).thenReturn("example-service");
        RBucket<String> bucket = mock();
        when(redis.<String>getBucket(anyString(), same(StringCodec.INSTANCE))).thenReturn(bucket);
        var serviceUrl = URL.valueOf("dubbo://localhost:20880/example.Service?group=demo");

        assertTrue(report.doGetExportedURLs(id).isEmpty());
        report.doSaveMetadata(id, serviceUrl);
        verify(bucket).set(URL.encode(serviceUrl.toFullString()));

        when(bucket.get()).thenReturn(URL.encode(serviceUrl.toFullString()));
        assertEquals(List.of(serviceUrl.toFullString()), report.doGetExportedURLs(id));
        report.doRemoveMetadata(id);
        verify(bucket).delete();
    }

    @Test
    void storesProviderConsumerAndSubscriberMetadataUnderTheirIdentifiers() {
        var key = org.apache.dubbo.metadata.report.identifier.KeyTypeEnum.UNIQUE_KEY;
        var provider = mock(org.apache.dubbo.metadata.report.identifier.MetadataIdentifier.class);
        var subscriber = mock(org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier.class);
        RBucket<String> definitions = mock();
        RBucket<String> subscriptions = mock();
        when(provider.getUniqueKey(key)).thenReturn("definition");
        when(subscriber.getUniqueKey(key)).thenReturn("subscription");
        when(redis.<String>getBucket("definition", StringCodec.INSTANCE)).thenReturn(definitions);
        when(redis.<String>getBucket("subscription", StringCodec.INSTANCE)).thenReturn(subscriptions);
        when(definitions.get()).thenReturn("provider");
        when(subscriptions.get()).thenReturn("urls");
        report.doStoreProviderMetadata(provider, "provider");
        report.doStoreConsumerMetadata(provider, "consumer");
        report.doSaveSubscriberData(subscriber, "urls");
        assertEquals("provider", report.getServiceDefinition(provider));
        assertEquals("urls", report.doGetSubscribedURLs(subscriber));
        verify(definitions).set("provider");
        verify(definitions).set("consumer");
        verify(subscriptions).set("urls");
    }

    @Test
    void readsPublishesAndRemovesApplicationMetadata() {
        var id = mock(org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier.class);
        var key = org.apache.dubbo.metadata.report.identifier.KeyTypeEnum.UNIQUE_KEY;
        var metadata = mock(org.apache.dubbo.metadata.MetadataInfo.class);
        RBucket<String> bucket = mock();
        when(id.getUniqueKey(key)).thenReturn("application");
        when(redis.<String>getBucket("application", StringCodec.INSTANCE)).thenReturn(bucket);
        when(metadata.getContent()).thenReturn("{}");
        when(bucket.get()).thenReturn("{}");
        report.publishAppMetadata(id, metadata);
        assertNotNull(report.getAppMetadata(id, java.util.Map.of()));
        report.unPublishAppMetadata(id, metadata);
        var storage = inOrder(bucket);
        storage.verify(bucket).set("{}");
        storage.verify(bucket).get();
        storage.verify(bucket).delete();
    }

    @Test
    void propagatesRedisReadFailureAsRpcFailure() {
        var id = mock(ServiceMetadataIdentifier.class);
        when(id.getIdentifierKey()).thenReturn("example-service");
        RBucket<String> bucket = mock();
        var storageError = new IllegalStateException("Redis unavailable");
        when(redis.<String>getBucket(anyString(), same(StringCodec.INSTANCE))).thenReturn(bucket);
        when(bucket.get()).thenThrow(storageError);

        assertSame(storageError, assertThrows(RpcException.class, () -> report.doGetExportedURLs(id)).getCause());
    }

    @Test
    void publishesMappingChangeOnlyWhenCompareAndSetSucceeds() {
        var script = mock(RScript.class);
        var topic = mock(RTopic.class);
        when(redis.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(redis.getTopic(anyString(), same(StringCodec.INSTANCE))).thenReturn(topic);
        when(script.<Long>eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.LONG),
            anyList(), eq("example.Service"), eq("app"), eq("ticket"))).thenReturn(0L, 1L);

        assertFalse(report.registerServiceAppMapping("example.Service", "mapping", "app", "ticket"));
        verifyNoInteractions(topic);
        assertTrue(report.registerServiceAppMapping("example.Service", "mapping", "app", "ticket"));
        verify(topic).publish("example.Service");
    }

    @Test
    void sharesTopicSubscriptionUntilTheLastMappingListenerIsRemoved() {
        var topic = mock(RTopic.class);
        RMap<String, String> mapping = mock();
        var first = mock(MappingListener.class);
        var second = mock(MappingListener.class);
        when(redis.getTopic(anyString(), same(StringCodec.INSTANCE))).thenReturn(topic);
        when(redis.<String, String>getMap(anyString(), same(StringCodec.INSTANCE))).thenReturn(mapping);
        when(mapping.get("example.Service")).thenReturn("app");

        assertTrue(report.getServiceAppMapping("example.Service", first, reportUrl).contains("app"));
        assertNotNull(report.getConfigItem("example.Service", "mapping"));
        report.getServiceAppMapping("another.Service", second, reportUrl);
        verify(topic).addListener(eq(String.class), any(org.redisson.api.listener.MessageListener.class));
        report.removeServiceAppMappingListener("example.Service", first);
        verify(topic, never()).removeAllListeners();
        report.removeServiceAppMappingListener("another.Service", second);
        verify(topic).removeAllListeners();
    }
}
