package org.dromara.gateway.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GatewayExceptionHandlerTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final GatewayExceptionHandler handler = new GatewayExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/users");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeAll
    static void initializeJsonUtils() {
        try (var beans = mockStatic(SpringUtil.class)) {
            beans.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(JSON);
            org.dromara.common.json.utils.JsonUtils.getJsonMapper();
        }
    }

    @Test
    void unavailableServiceReturnsNotFound() throws IOException {
        handler.handle(request, response, new IllegalStateException("Unable to find instance for system"));
        assertResponse(404, "服务未找到");
    }

    @Test
    void responseStatusExceptionPreservesItsStatusAndMessage() throws IOException {
        var error = new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "request quota reached");
        handler.handle(request, response, error);
        assertResponse(429, error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"internal storage details", ""})
    void unexpectedFailureReturnsPublicMessage(String detail) throws IOException {
        handler.handle(request, response, new IllegalStateException(detail.isEmpty() ? null : detail));
        assertResponse(500, "内部服务器错误");
    }

    @Test
    void missingSessionReturnsUnauthorizedJson() throws IOException {
        var error = mock(NotLoginException.class);
        when(error.getMessage()).thenReturn("login required");
        handler.handleNotLogin(request, response, error);
        assertResponse(401, "login required");
    }

    private void assertResponse(int status, String message) throws IOException {
        assertEquals(status, response.getStatus());
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertTrue(response.getContentType().startsWith("application/json"));
        var payload = JSON.readTree(response.getContentAsString());
        assertEquals(status, payload.path("code").asInt());
        assertEquals(message, payload.path("msg").asString());
    }
}
