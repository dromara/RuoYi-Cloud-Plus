package org.dromara.common.seata;

import com.alibaba.cloud.seata.rest.SeataRestTemplateInterceptor;
import com.alibaba.cloud.seata.web.SeataHandlerInterceptor;
import org.apache.seata.core.context.RootContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeataContextPropagationTest {

    @AfterEach
    void clearTransactionContext() {
        RootContext.unbind();
    }

    @Test
    void bindsIncomingTransactionAndUnbindsAtCompletion() {
        var request = new MockHttpServletRequest();
        request.addHeader(RootContext.KEY_XID, "incoming-xid");
        var response = new MockHttpServletResponse();
        var interceptor = new SeataHandlerInterceptor();

        assertTrue(interceptor.preHandle(request, response, this));
        assertEquals("incoming-xid", RootContext.getXID());
        interceptor.afterCompletion(request, response, this, null);
        assertNull(RootContext.getXID());
    }

    @Test
    void preservesAnExistingTransactionAcrossIncomingRequests() {
        RootContext.bind("local-xid");
        var interceptor = new SeataHandlerInterceptor();
        var response = new MockHttpServletResponse();
        var request = new MockHttpServletRequest();

        interceptor.afterCompletion(request, response, this, null);
        assertEquals("local-xid", RootContext.getXID());
        request.addHeader(RootContext.KEY_XID, "different-xid");
        interceptor.preHandle(request, response, this);
        assertEquals("local-xid", RootContext.getXID());
        interceptor.afterCompletion(request, response, this, null);
        assertEquals("local-xid", RootContext.getXID());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void forwardsTheActiveTransactionAlongsideTheOriginalRequestBody(boolean transactional) throws Exception {
        if (transactional) {
            RootContext.bind("outgoing-xid");
        }
        var request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        var body = new byte[]{1, 2};
        var expected = mock(ClientHttpResponse.class);

        var result = new SeataRestTemplateInterceptor().intercept(request, body, (forwarded, payload) -> {
            assertEquals(transactional ? "outgoing-xid" : null,
                forwarded.getHeaders().getFirst(RootContext.KEY_XID));
            assertSame(body, payload);
            return expected;
        });

        assertSame(expected, result);
    }
}
