package org.dromara.resource.service.impl;

import org.dromara.resource.dubbo.RemoteMessageServiceImpl;
import org.dromara.resource.api.domain.RemotePushPayLoad;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.push.helper.PushHelper;
import org.dromara.common.push.dto.PushPayloadDTO;
import org.dromara.resource.mapper.SysMessageMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SysMessageServiceImplTest {

    private static RemotePushPayLoad remotePayload(PushPayloadDTO payload) {
        var remote = RemotePushPayLoad.of(payload.getType(), payload.getSource(), payload.getMessage(), payload.getData());
        remote.setMessageId(payload.getMessageId() == null ? 17L : payload.getMessageId());
        return remote;
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void messageBoxInsertFailureStopsDelivery(boolean broadcast) {
        var mapper = mock(SysMessageMapper.class);
        var service = new SysMessageServiceImpl(mapper);
        var payload = PushPayloadDTO.of("message", "backend", "提醒", null);
        payload.setMessageId(7L);
        try (var spring = mockStatic(SpringUtil.class); var push = mockStatic(PushHelper.class)) {
            spring.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(JsonMapper.builder().build());
            assertThrows(ServiceException.class, () -> {
                if (broadcast) {
                    new RemoteMessageServiceImpl(service).publishAllPayload(remotePayload(payload));
                } else {
                    new RemoteMessageServiceImpl(service).publishMessagePayload(List.of(42L), remotePayload(payload));
                }
            });
            push.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    void targetedMessagesRequireRecipients(List<Long> recipients) {
        var mapper = mock(SysMessageMapper.class);
        var service = new SysMessageServiceImpl(mapper);
        var payload = PushPayloadDTO.of("message", "backend", "提醒", null);
        payload.setMessageId(7L);
        try (var spring = mockStatic(SpringUtil.class); var push = mockStatic(PushHelper.class)) {
            assertSame(payload, service.storeUsers(recipients, payload));
            new RemoteMessageServiceImpl(service).publishMessagePayload(recipients, remotePayload(payload));
            verifyNoInteractions(mapper);
            push.verifyNoInteractions();
        }
    }
}
