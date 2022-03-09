package com.ruoyi.common.web.config;

import com.ruoyi.common.web.core.I18nLocaleResolver;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

/**
 * 国际化配置
 *
 * @author Lion Li
 */
@Configuration
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
public class I18nConfig {

    @Bean
    public LocaleResolver localeResolver() {
        return new I18nLocaleResolver();
    }

}
