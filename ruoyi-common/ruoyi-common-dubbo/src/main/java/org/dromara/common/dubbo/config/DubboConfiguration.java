package org.dromara.common.dubbo.config;

import org.dromara.common.core.factory.YmlPropertySourceFactory;
import org.dromara.common.dubbo.properties.DubboCustomProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

/**
 * dubbo 配置类
 */
@AutoConfiguration
@EnableConfigurationProperties(DubboCustomProperties.class)
@PropertySource(value = "classpath:common-dubbo.yml", factory = YmlPropertySourceFactory.class)
public class DubboConfiguration {

}
