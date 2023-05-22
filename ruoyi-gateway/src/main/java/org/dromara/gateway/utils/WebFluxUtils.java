package org.dromara.gateway.utils;

import cn.hutool.core.util.ObjectUtil;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.gateway.filter.GlobalCacheRequestFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR;

/**
 * WebFlux 工具类
 *
 * @author Lion Li
 */
public class WebFluxUtils {

    /**
     * 获取原请求路径
     */
    public static String getOriginalRequestUrl(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        LinkedHashSet<URI> uris = exchange.getAttributeOrDefault(GATEWAY_ORIGINAL_REQUEST_URL_ATTR, new LinkedHashSet<>());
        URI requestUri = uris.stream().findFirst().orElse(request.getURI());
        return UriComponentsBuilder.fromPath(requestUri.getRawPath()).build().toUriString();
    }

    /**
     * 是否是Json请求
     *
     * @param exchange HTTP请求
     */
    public static boolean isJsonRequest(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        return StringUtils.startsWithIgnoreCase(header, MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * 读取request内的body
     *
     * 注意一个request只能读取一次 读取之后需要重新包装
     */
    public static String resolveBodyFromRequest(ServerHttpRequest serverHttpRequest) {
        // 获取请求体
        Flux<DataBuffer> body = serverHttpRequest.getBody();
        AtomicReference<String> bodyRef = new AtomicReference<>();
        body.subscribe(buffer -> {
            try (DataBuffer.ByteBufferIterator iterator = buffer.readableByteBuffers()) {
                CharBuffer charBuffer = StandardCharsets.UTF_8.decode(iterator.next());
                DataBufferUtils.release(buffer);
                bodyRef.set(charBuffer.toString());
            }
        });
        return bodyRef.get();
    }

    /**
     * 从缓存中读取request内的body
     *
     * 注意要求经过 {@link ServerWebExchangeUtils#cacheRequestBody(ServerWebExchange, Function)} 此方法创建缓存
     * 框架内已经使用 {@link GlobalCacheRequestFilter} 全局创建了body缓存
     *
     * @return body
     */
    public static String resolveBodyFromCacheRequest(ServerWebExchange exchange) {
        Object obj = exchange.getAttributes().get(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
        if (ObjectUtil.isNull(obj)) {
            return null;
        }
        DataBuffer buffer = (DataBuffer) obj;
        try (DataBuffer.ByteBufferIterator iterator = buffer.readableByteBuffers()) {
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(iterator.next());
            return charBuffer.toString();
        }
    }

    /**
     * 设置webflux模型响应
     *
     * @param response ServerHttpResponse
     * @param value    响应内容
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, Object value) {
        return webFluxResponseWriter(response, HttpStatus.OK, value, R.FAIL);
    }

    /**
     * 设置webflux模型响应
     *
     * @param response ServerHttpResponse
     * @param code     响应状态码
     * @param value    响应内容
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, Object value, int code) {
        return webFluxResponseWriter(response, HttpStatus.OK, value, code);
    }

    /**
     * 设置webflux模型响应
     *
     * @param response ServerHttpResponse
     * @param status   http状态码
     * @param code     响应状态码
     * @param value    响应内容
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, HttpStatus status, Object value, int code) {
        return webFluxResponseWriter(response, MediaType.APPLICATION_JSON_VALUE, status, value, code);
    }

    /**
     * 设置webflux模型响应
     *
     * @param response    ServerHttpResponse
     * @param contentType content-type
     * @param status      http状态码
     * @param code        响应状态码
     * @param value       响应内容
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, String contentType, HttpStatus status, Object value, int code) {
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, contentType);
        R<?> result = R.fail(code, value.toString());
        DataBuffer dataBuffer = response.bufferFactory().wrap(JsonUtils.toJsonString(result).getBytes());
        return response.writeWith(Mono.just(dataBuffer));
    }
}
