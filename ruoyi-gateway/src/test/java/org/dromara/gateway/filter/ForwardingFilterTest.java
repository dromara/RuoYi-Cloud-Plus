package org.dromara.gateway.filter;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.same.SaSameUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForwardingFilterTest {

    @Test
    void addsServicePrefixAndPreservesMultiValueHeaders() {
        var request = mock(ServerRequest.class);
        when(request.servletRequest()).thenReturn(new MockHttpServletRequest("GET", "/system/user/list"));
        var headers = new HttpHeaders();
        headers.addAll("Accept", List.of("application/json", "text/plain"));

        var forwarded = new ForwardHeadersFilter().apply(headers, request);

        assertEquals("/system", forwarded.getFirst("X-Forwarded-Prefix"));
        assertEquals(headers.get("Accept"), forwarded.get("Accept"));
        assertNull(headers.getFirst("X-Forwarded-Prefix"));
    }

    @Test
    void retainsOriginalHeadersForRootRequest() {
        var request = mock(ServerRequest.class);
        when(request.servletRequest()).thenReturn(new MockHttpServletRequest("GET", "/"));
        var headers = new HttpHeaders();

        assertSame(headers, new ForwardHeadersFilter().apply(headers, request));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void forwardsInternalTokenAccordingToConfiguration(boolean enabled) throws Exception {
        var request = new MockHttpServletRequest("GET", "/system/user");
        request.addHeader("Accept", "application/json");
        request.addHeader(SaSameUtil.SAME_TOKEN, "incoming");
        try (var manager = mockStatic(SaManager.class); var tokens = mockStatic(SaSameUtil.class)) {
            manager.when(SaManager::getConfig).thenReturn(new SaTokenConfig().setCheckSameToken(enabled));
            tokens.when(SaSameUtil::getToken).thenReturn("internal");

            new ForwardAuthFilter().doFilter(request, new MockHttpServletResponse(), (downstream, response) -> {
                var forwarded = (HttpServletRequest) downstream;
                assertEquals(enabled ? "internal" : "incoming", forwarded.getHeader(SaSameUtil.SAME_TOKEN));
                assertEquals(List.of(enabled ? "internal" : "incoming"),
                    Collections.list(forwarded.getHeaders(SaSameUtil.SAME_TOKEN)));
                assertEquals(List.of("Accept", SaSameUtil.SAME_TOKEN), Collections.list(forwarded.getHeaderNames()));
                assertEquals("application/json", forwarded.getHeader("Accept"));
            });
            tokens.verify(SaSameUtil::getToken, times(enabled ? 1 : 0));
        }
    }

    @Test
    void actuatorRequestPassesThroughWithoutTokenLookup() throws Exception {
        var request = new MockHttpServletRequest("GET", "/actuator/health");
        try (var tokens = mockStatic(SaSameUtil.class)) {
            new ForwardAuthFilter().doFilter(request, new MockHttpServletResponse(),
                (downstream, response) -> assertSame(request, downstream));
            tokens.verifyNoInteractions();
        }
    }
}
