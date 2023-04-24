package org.dromara.common.loadbalance.config;

import org.dromara.common.loadbalance.core.CustomSpringCloudLoadBalancer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 自定义负载均衡客户端配置
 *
 * @author LionLi
 */
@SuppressWarnings("all")
@Configuration(proxyBeanMethods = false)
public class CustomLoadBalanceClientConfiguration {

    @Bean
    @ConditionalOnBean(LoadBalancerClientFactory.class)
    public ReactorLoadBalancer<ServiceInstance> customLoadBalancer(Environment environment,
                                                                   LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new CustomSpringCloudLoadBalancer(name,
            loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class));
    }
}
