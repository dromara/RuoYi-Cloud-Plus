package org.dromara.common.dubbo.properties;

import lombok.Data;
import org.dromara.common.dubbo.enumd.RequestLogEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

/**
 * 自定义配置
 *
 * @author Lion Li
 */
@Data
@RefreshScope
@ConfigurationProperties(prefix = "dubbo.custom")
public class DubboCustomProperties {

    /**
     * 是否开启请求日志记录
     */
    private Boolean requestLog;

    /**
     * 日志级别
     */
    private RequestLogEnum logLevel;

}
