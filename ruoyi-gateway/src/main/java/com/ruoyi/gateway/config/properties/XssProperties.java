package com.ruoyi.gateway.config.properties;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * XSS跨站脚本配置
 * 
 * @author ruoyi
 */
@Data
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "security.xss")
public class XssProperties
{
    /**
     * Xss开关
     */
    private Boolean enabled;

    /**
     * 排除路径
     */
    private List<String> excludeUrls = new ArrayList<>();

}
