package org.dromara.common.job.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "snail-job.server")
public class SnailJobServerProperties {

    private String serverName;

    private String port;

}
