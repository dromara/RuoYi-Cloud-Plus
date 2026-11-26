package org.dromara.gateway.filter;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.gateway.config.properties.ApiDecryptProperties;
import org.dromara.gateway.config.properties.CustomGatewayProperties;
import org.dromara.gateway.filter.support.CachedBodyHttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class GlobalLogFilterTest {

    @ParameterizedTest
    @ValueSource(strings = {"valid", "invalid", "encrypted", "empty"})
    void preservesRequestBodyForDownstreamReaders(String format) throws Exception {
        var properties = new CustomGatewayProperties();
        properties.setRequestLog(true);
        var decrypt = new ApiDecryptProperties();
        decrypt.setEnabled("encrypted".equals(format));
        decrypt.setHeaderFlag("encrypt-key");
        var request = new MockHttpServletRequest("POST", "/system/example");
        request.setContentType("application/json");
        if ("encrypted".equals(format)) {
            request.addHeader("encrypt-key", "placeholder");
        }
        String body = switch (format) {
            case "valid" -> "{\"name\":\"测试\",\"password\":\"placeholder\"}";
            case "invalid" -> "{";
            case "encrypted" -> "ciphertext";
            default -> "";
        };
        request.setContent(body.getBytes(StandardCharsets.UTF_8));

        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(JsonMapper.builder().build());
            new GlobalLogFilter(properties, decrypt).doFilter(request, new MockHttpServletResponse(),
                (forwarded, response) -> {
                    var cached = assertInstanceOf(CachedBodyHttpServletRequest.class, forwarded);
                    assertArrayEquals(body.getBytes(StandardCharsets.UTF_8), cached.getInputStream().readAllBytes());
                    assertEquals(body.isEmpty() ? null : body, cached.getReader().readLine());
                });
        }
    }

    @Test
    void loggingDisabledPassesTheOriginalRequestThrough() throws Exception {
        var properties = new CustomGatewayProperties();
        properties.setRequestLog(false);
        var request = new MockHttpServletRequest("POST", "/system/example");
        request.setContentType("application/json");

        new GlobalLogFilter(properties, new ApiDecryptProperties()).doFilter(request, new MockHttpServletResponse(),
            (forwarded, response) -> assertSame(request, forwarded));
    }
}
