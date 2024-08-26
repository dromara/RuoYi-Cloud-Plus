package org.dromara.common.sentinel.config;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.cloud.sentinel.SentinelProperties;
import com.alibaba.cloud.sentinel.custom.SentinelAutoConfiguration;
import com.alibaba.csp.sentinel.init.InitExecutor;
import com.alibaba.csp.sentinel.transport.config.TransportConfig;
import org.dromara.common.core.utils.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @author Lion Li
 */
@AutoConfiguration(before = SentinelAutoConfiguration.class)
@EnableConfigurationProperties({SentinelProperties.class, SentinelCustomProperties.class})
public class SentinelCustomAutoConfiguration {

    @Autowired
    private SentinelProperties properties;
    @Autowired
    private SentinelCustomProperties customProperties;
    @Autowired
    private DiscoveryClient discoveryClient;

    @Bean
    public void sentinelInit() {
        if (StringUtils.isNotBlank(customProperties.getServerName())) {
            List<ServiceInstance> instances = discoveryClient.getInstances(customProperties.getServerName());
            String serverList = StreamUtils.join(instances, instance ->
                String.format("http://%s:%s", instance.getHost(), instance.getPort()));
            System.setProperty(TransportConfig.CONSOLE_SERVER, serverList);
        } else {
            if (StringUtils.isEmpty(System.getProperty(TransportConfig.CONSOLE_SERVER))
                && StringUtils.isNotBlank(properties.getTransport().getDashboard())) {
                System.setProperty(TransportConfig.CONSOLE_SERVER,
                    properties.getTransport().getDashboard());
            }
        }
        // 手动初始化 sentinel
        InitExecutor.doInit();
    }


}
