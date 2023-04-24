package org.dromara.common.job.config;

import cn.hutool.core.collection.CollUtil;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.job.config.properties.XxlJobProperties;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * xxl-job config
 *
 * @author Lion Li
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(XxlJobProperties.class)
@AllArgsConstructor
@ConditionalOnProperty(prefix = "xxl.job", name = "enabled", havingValue = "true")
public class XxlJobConfig {

    private final XxlJobProperties xxlJobProperties;

    private final DiscoveryClient discoveryClient;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info(">>>>>>>>>>> xxl-job config init.");
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        if (StringUtils.isNotBlank(xxlJobProperties.getAdminAppname())) {
            List<ServiceInstance> instances = discoveryClient.getInstances(xxlJobProperties.getAdminAppname());
            if (CollUtil.isEmpty(instances)) {
                throw new RuntimeException("调度中心不存在!");
            }
            String serverList = StreamUtils.join(instances, instance ->
                String.format("http://%s:%s", instance.getHost(), instance.getPort()));
            xxlJobSpringExecutor.setAdminAddresses(serverList);
        } else {
            xxlJobSpringExecutor.setAdminAddresses(xxlJobProperties.getAdminAddresses());
        }
        xxlJobSpringExecutor.setAccessToken(xxlJobProperties.getAccessToken());
        XxlJobProperties.Executor executor = xxlJobProperties.getExecutor();
        xxlJobSpringExecutor.setAppname(executor.getAppname());
        xxlJobSpringExecutor.setAddress(executor.getAddress());
        xxlJobSpringExecutor.setIp(executor.getIp());
        xxlJobSpringExecutor.setPort(executor.getPort());
        xxlJobSpringExecutor.setLogPath(executor.getLogPath());
        xxlJobSpringExecutor.setLogRetentionDays(executor.getLogRetentionDays());
        return xxlJobSpringExecutor;
    }

}
