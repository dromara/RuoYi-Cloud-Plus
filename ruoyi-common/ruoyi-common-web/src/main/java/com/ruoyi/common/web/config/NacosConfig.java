package com.ruoyi.common.web.config;

import com.alibaba.cloud.nacos.ConditionalOnNacosDiscoveryEnabled;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryClientConfiguration;
import com.alibaba.cloud.nacos.discovery.NacosWatch;
import com.ruoyi.common.web.nacos.CustomNacosWatch;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.context.annotation.Bean;

/**
 * 自定义 nacos 监听 解决与 Undertow 整合报错问题
 *
 * @author Lion Li
 */
@AutoConfiguration(
    before = NacosDiscoveryClientConfiguration.class,
    after = NacosDiscoveryAutoConfiguration.class
)
@ConditionalOnDiscoveryEnabled
@ConditionalOnBlockingDiscoveryEnabled
@ConditionalOnNacosDiscoveryEnabled
public class NacosConfig {

    @Bean
    @ConditionalOnProperty(value = "spring.cloud.nacos.discovery.watch.enabled", matchIfMissing = true)
    public NacosWatch nacosWatch(NacosServiceManager nacosServiceManager,
                                 NacosDiscoveryProperties nacosDiscoveryProperties) {
        return new CustomNacosWatch(nacosServiceManager, nacosDiscoveryProperties);
    }

}
