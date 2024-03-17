package org.dromara.common.job.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "easy-retry.server")
public class EasyRetryServerProperties {

    private String serverName;

    private String port;

}
