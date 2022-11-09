package com.ruoyi.gateway.filter;

import cn.dev33.satoken.same.SaSameUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 转发认证过滤器(内部服务外网隔离)
 *
 * @author Lion Li
 */
@Component
public class ForwardAuthFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest newRequest = exchange
            .getRequest()
            .mutate()
            // 为请求追加 Same-Token 参数
            .header(SaSameUtil.SAME_TOKEN, SaSameUtil.getToken())
            .build();
        ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
        return chain.filter(newExchange);
    }
}

