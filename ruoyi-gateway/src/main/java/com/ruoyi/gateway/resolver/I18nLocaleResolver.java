package com.ruoyi.gateway.resolver;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.i18n.LocaleContextResolver;

import java.util.Locale;

/**
 * 获取请求头国际化信息
 *
 * @author Lion Li
 */
public class I18nLocaleResolver implements LocaleContextResolver {

    @Override
    public LocaleContext resolveLocaleContext(ServerWebExchange exchange) {
        String language = exchange.getRequest().getHeaders().getFirst("content-language");
        Locale locale = Locale.getDefault();
        if (language != null && language.length() > 0) {
            String[] split = language.split("_");
            locale = new Locale(split[0], split[1]);
        }
        return new SimpleLocaleContext(locale);
    }

    @Override
    public void setLocaleContext(ServerWebExchange exchange, LocaleContext localeContext) {

    }
}
