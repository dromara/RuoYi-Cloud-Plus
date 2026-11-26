package org.dromara.gateway.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class GatewayRequestPolicyTest {

    private final GatewayConfig config = new GatewayConfig();

    @ParameterizedTest
    @CsvSource({"zh_CN,zh-CN", "en-GB,en-GB", "zh_Hant_TW,zh-Hant-TW"})
    void resolvesLegacyAndStandardLanguageTags(String header, String expected) {
        var request = new MockHttpServletRequest();
        request.addHeader("Content-Language", header);
        assertEquals(expected, config.localeResolver().resolveLocale(request).toLanguageTag());
    }

    @Test
    void resolvesMissingLanguageToTheDefaultLocale() {
        assertEquals(Locale.getDefault(), config.localeResolver().resolveLocale(new MockHttpServletRequest()));
    }

    @ParameterizedTest
    @CsvSource({"POST,200", "TRACE,403"})
    void corsPreflightEnforcesTheConfiguredMethods(String method, int expectedStatus) throws Exception {
        var request = new MockHttpServletRequest("OPTIONS", "/system/example");
        request.addHeader("Origin", "https://app.example.org");
        request.addHeader("Access-Control-Request-Method", method);
        request.addHeader("Access-Control-Request-Headers", "Authorization");
        var response = new MockHttpServletResponse();

        config.corsFilterRegistrationBean().getFilter().doFilter(request, response,
            (forwarded, result) -> fail("Preflight should be resolved at the gateway"));

        assertEquals(expectedStatus, response.getStatus());
        if (expectedStatus == 200) {
            assertEquals("https://app.example.org", response.getHeader("Access-Control-Allow-Origin"));
            assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
            assertTrue(response.getHeader("Access-Control-Allow-Headers").contains("Authorization"));
        }
    }
}
