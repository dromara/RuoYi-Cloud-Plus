package org.dromara.common.loadbalance.core;

import cn.hutool.core.net.NetUtil;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.dromara.common.loadbalance.config.CustomEnvironmentPostProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.core.Ordered;
import org.springframework.core.env.StandardEnvironment;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("common-loadbalance 功能单元测试")
class LoadBalanceFunctionTest {

    /**
     * 本机 IPv4 地址，用于验证同实例优先策略。
     */
    private static final String LOCAL_IP = NetUtil.localIpv4s().iterator().next();

    private static final String REMOTE_IP = "10.255.255.1";

    @AfterEach
    void cleanUp() {
        System.clearProperty("dubbo.consumer.loadbalance");
    }

    /**
     * 验证 Dubbo 负载均衡优先选择本机实例，非本机实例时在列表内随机选择。
     */
    @Test
    @DisplayName("Dubbo 负载均衡优先本机实例")
    @SuppressWarnings("unchecked")
    void shouldPreferLocalInvokerForDubbo() {
        Invoker<Object> local = dubboInvoker(LOCAL_IP);
        Invoker<Object> remote = dubboInvoker(REMOTE_IP);
        CustomDubboLoadBalancer balancer = new CustomDubboLoadBalancer();
        Invocation invocation = mock(Invocation.class);

        assertSame(local, balancer.doSelect(List.of(remote, local), URL.valueOf("dubbo://" + LOCAL_IP + ":20880/T"), invocation));
        assertEquals(remote, balancer.doSelect(List.of(remote), URL.valueOf("dubbo://" + LOCAL_IP + ":20880/T"), invocation));
    }

    /**
     * 验证 SpringCloud 负载均衡在实例列表为空时返回空响应。
     */
    @Test
    @DisplayName("SpringCloud 负载均衡空实例返回空响应")
    void shouldReturnEmptyResponseWhenNoInstances() {
        Response<ServiceInstance> response = choose(List.of());

        assertNotNull(response);
        assertFalse(response.hasServer());
    }

    /**
     * 验证 SpringCloud 负载均衡优先选择本机实例。
     */
    @Test
    @DisplayName("SpringCloud 负载均衡优先本机实例")
    void shouldPreferLocalInstanceForSpringCloud() {
        ServiceInstance local = instance("local", LOCAL_IP);
        ServiceInstance remote = instance("remote", REMOTE_IP);

        Response<ServiceInstance> response = choose(List.of(remote, local));

        assertTrue(response.hasServer());
        assertSame(local, response.getServer());
    }

    /**
     * 验证环境后置处理器注入自定义 Dubbo 负载均衡算法并保持最高优先级。
     */
    @Test
    @DisplayName("注入自定义负载均衡系统属性")
    void shouldInjectCustomLoadBalanceSystemProperty() {
        CustomEnvironmentPostProcessor processor = new CustomEnvironmentPostProcessor();

        assertEquals(Ordered.HIGHEST_PRECEDENCE, processor.getOrder());
        processor.postProcessEnvironment(new StandardEnvironment(), new SpringApplication(Object.class));

        assertEquals("customDubboLoadBalancer", System.getProperty("dubbo.consumer.loadbalance"));
    }

    private static <T> Invoker<T> dubboInvoker(String host) {
        return dubboInvoker(host, URL.valueOf("dubbo://" + host + ":20880/org.Foo"));
    }

    @SuppressWarnings("unchecked")
    private static <T> Invoker<T> dubboInvoker(String host, URL url) {
        Invoker<T> invoker = mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }

    private static ServiceInstance instance(String instanceId, String host) {
        return new DefaultServiceInstance(instanceId, "service", host, 8080, false);
    }

    @SuppressWarnings("unchecked")
    private static Response<ServiceInstance> choose(List<ServiceInstance> instances) {
        ObjectProvider<ServiceInstanceListSupplier> provider = mock(ObjectProvider.class);
        ServiceInstanceListSupplier supplier = mock(ServiceInstanceListSupplier.class);
        when(provider.getIfAvailable(any())).thenReturn(supplier);
        when(supplier.get(any())).thenReturn(Flux.just(instances));

        CustomSpringCloudLoadBalancer balancer = new CustomSpringCloudLoadBalancer("service", provider);
        return balancer.choose(mock(Request.class)).block();
    }

}
