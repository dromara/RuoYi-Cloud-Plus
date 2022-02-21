package com.ruoyi.gateway.config;

import com.ruoyi.gateway.resolver.I18nLocaleResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration;
import org.springframework.web.server.i18n.LocaleContextResolver;

/**
 * webflux 国际化解析器
 *
 * @author Lion Li
 */
@Configuration
public class I18nConfig extends DelegatingWebFluxConfiguration {

    @Override
    protected LocaleContextResolver createLocaleContextResolver() {
        return new I18nLocaleResolver();
    }

}
