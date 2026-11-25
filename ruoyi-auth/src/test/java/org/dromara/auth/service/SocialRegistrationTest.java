package org.dromara.auth.service;

import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.domain.bo.RemoteSocialBo;
import org.dromara.system.api.domain.vo.RemoteSocialVo;
import org.dromara.system.api.RemoteSocialService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SocialRegistrationTest {
    private final RemoteSocialService bindings = mock();
    private final SysLoginService login = new SysLoginService(mock());

    @org.junit.jupiter.api.BeforeEach
    void injectSocialService() {
        org.springframework.test.util.ReflectionTestUtils.setField(login, "remoteSocialService", bindings);
    }

    private AuthUser account() {
        return AuthUser.builder().source("GITHUB").uuid("123").username("alice")
            .token(AuthToken.builder().accessToken("test-token").build()).build();
    }

    @Test
    void alreadyBoundExternalAccountRejectsBindingBeforeAnyWrite() {
        when(bindings.selectByAuthId("GITHUB123")).thenReturn(List.of(new RemoteSocialVo()));
        try (var session = mockStatic(LoginHelper.class)) {
            session.when(LoginHelper::getUserId).thenReturn(51L);
            assertThrows(ServiceException.class, () -> login.socialRegister(account()));
            verify(bindings).selectByAuthId("GITHUB123");
            verifyNoMoreInteractions(bindings);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void freeExternalAccountUsesExistingBindingOnlyForSameUserAndPlatform(boolean existing) {
        var previous = new RemoteSocialVo();
        previous.setId(71L);
        when(bindings.queryList(argThat(query -> Long.valueOf(51L).equals(query.getUserId())
            && "GITHUB".equals(query.getSource())))).thenReturn(existing ? List.of(previous) : List.of());
        try (var session = mockStatic(LoginHelper.class)) {
            session.when(LoginHelper::getUserId).thenReturn(51L);
            login.socialRegister(account());
            var saved = ArgumentCaptor.forClass(RemoteSocialBo.class);
            verify(bindings).selectByAuthId("GITHUB123");
            verify(bindings).queryList(argThat(query -> Long.valueOf(51L).equals(query.getUserId())
                && "GITHUB".equals(query.getSource())));
            if (existing) {
                verify(bindings).updateByBo(saved.capture());
                assertEquals(71L, saved.getValue().getId());
            } else {
                verify(bindings).insertByBo(saved.capture());
                assertNull(saved.getValue().getId());
            }
            verifyNoMoreInteractions(bindings);
            assertEquals(51L, saved.getValue().getUserId());
            assertEquals("GITHUB123", saved.getValue().getAuthId());
            assertEquals("123", saved.getValue().getOpenId());
        }
    }
}
