package org.dromara.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;


/**
 * 跨域配置
 *
 * @author Lion Li
 */
@Component
public class GlobalCorsFilter implements WebFilter, Ordered {

    /**
     * 这里为支持的请求头，如果有自定义的header字段请自己添加
     */
    private static final String ALLOWED_HEADERS =
        "X-Requested-With, Content-Language, Content-Type, " +
        "Authorization, clientid, credential, X-XSRF-TOKEN, " +
        "isToken, token, Admin-Token, App-Token, Encrypt-Key, isEncrypt";

    /**
     * 允许的请求方法
     */
    private static final String ALLOWED_METHODS = "GET,POST,PUT,DELETE,OPTIONS,HEAD";

    /**
     * 允许的请求来源，使用 * 表示允许任何来源
     */
    private static final String ALLOWED_ORIGIN = "*";

    /**
     * 允许前端访问的响应头，使用 * 表示允许任何响应头
     */
    private static final String ALLOWED_EXPOSE = "*";

    /**
     * 预检请求的缓存时间，单位为秒（此处设置为 5 小时）
     */
    private static final String MAX_AGE = "18000L";

    /**
     * 实现跨域配置的 Web 过滤器
     *
     * @param exchange ServerWebExchange 对象，表示一次 Web 交换
     * @param chain    WebFilterChain 对象，表示一组 Web 过滤器链
     * @return Mono<Void> 表示异步的过滤器链处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // 判断请求是否为跨域请求
        if (CorsUtils.isCorsRequest(request)) {
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();
            headers.add("Access-Control-Allow-Headers", ALLOWED_HEADERS);
            headers.add("Access-Control-Allow-Methods", ALLOWED_METHODS);
            headers.add("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
            headers.add("Access-Control-Expose-Headers", ALLOWED_EXPOSE);
            headers.add("Access-Control-Max-Age", MAX_AGE);
            headers.add("Access-Control-Allow-Credentials", "true");
            // 处理预检请求的 OPTIONS 方法，直接返回成功状态码
            if (request.getMethod() == HttpMethod.OPTIONS) {
                response.setStatusCode(HttpStatus.OK);
                return Mono.empty();
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
