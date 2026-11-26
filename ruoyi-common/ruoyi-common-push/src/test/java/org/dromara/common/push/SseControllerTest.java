package org.dromara.common.push;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.core.domain.R;
import org.dromara.common.push.controller.SseController;
import org.dromara.common.push.core.SseEmitterSessionManager;
import org.dromara.common.satoken.utils.LoginHelper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SseControllerTest {
    @Test
    void destroyingControllerLeavesSessionLifecycleToTheManager() throws Exception {
        var sessions = mock(SseEmitterSessionManager.class);
        new SseController(sessions).destroy();
        verifyNoInteractions(sessions);
    }

    @Test
    void opensAndClosesTheSameSessionWithStreamingHeaders() {
        var sessions = mock(SseEmitterSessionManager.class);
        var expected = new SseEmitter();
        when(sessions.connect(42L, "session")).thenReturn(expected);
        var controller = new SseController(sessions);
        var response = new MockHttpServletResponse();
        try (var login = mockStatic(LoginHelper.class); var token = mockStatic(StpUtil.class)) {
            login.when(LoginHelper::getUserId).thenReturn(42L);
            token.when(StpUtil::getTokenValue).thenReturn("session");
            assertSame(expected, controller.connect(response));
            assertTrue(response.getContentType().startsWith("text/event-stream"));
            assertEquals("UTF-8", response.getCharacterEncoding());
            assertEquals("no-cache", response.getHeader("Cache-Control"));
            assertEquals("no", response.getHeader("X-Accel-Buffering"));
            assertTrue(R.isSuccess(controller.close()));
            var order = inOrder(sessions);
            order.verify(sessions).connect(42L, "session");
            order.verify(sessions).disconnect(42L, "session");
        }
    }
}
