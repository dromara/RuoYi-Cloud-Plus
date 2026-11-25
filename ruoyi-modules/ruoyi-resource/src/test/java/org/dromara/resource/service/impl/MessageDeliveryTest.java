package org.dromara.resource.service.impl;

import org.dromara.resource.dubbo.RemoteMessageServiceImpl;
import org.dromara.resource.api.domain.RemotePushPayLoad;
import org.dromara.common.push.dto.PushPayloadDTO;
import org.dromara.resource.domain.SysMessage;
import org.dromara.resource.mapper.SysMessageMapper;
import org.dromara.common.push.helper.PushHelper;
import cn.hutool.extra.spring.SpringUtil;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageDeliveryTest {

    private static RemotePushPayLoad remotePayload(PushPayloadDTO payload) {
        var remote = RemotePushPayLoad.of(payload.getType(), payload.getSource(), payload.getMessage(), payload.getData());
        remote.setPath(payload.getPath());
        remote.setMessageId(payload.getMessageId() == null ? 17L : payload.getMessageId());
        return remote;
    }


    @Test
    void missingMessageContentUsesCategoryTitleAndLeavesBodyEmpty() {
        var mapper = mock(SysMessageMapper.class);
        var payload = PushPayloadDTO.of("message", "backend", "提醒", null);
        payload.setMessageId(17L);
        when(mapper.insert(any(SysMessage.class))).thenReturn(1);
        try (var beans = mockStatic(SpringUtil.class)) {
            beans.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(JsonMapper.builder().build());
            new SysMessageServiceImpl(mapper).storeAll(payload);
            var record = ArgumentCaptor.forClass(SysMessage.class);
            verify(mapper).insert(record.capture());
            assertEquals("系统消息", record.getValue().getTitle());
            assertEquals("提醒", record.getValue().getMessage());
            assertNull(record.getValue().getContent());
        }
    }

    @ParameterizedTest
    @CsvSource({"notice,workflow,notice,通知公告消息,true","message,workflow,workflow,工作流消息,false",
        "message,backend,system,系统消息,false","message,notice,notice,通知公告消息,true"})
    void persistsRecipientScopeAndCategoryBeforePushing(String type, String source, String category,
                                                        String title, boolean broadcast) {
        var messages = mock(SysMessageMapper.class);
        var service = new SysMessageServiceImpl(messages);
        var payload = PushPayloadDTO.of(type, source, "处理通知", Map.of("noticeContent", "正文"));
        payload.setPath("/detail");
        payload.setMessageId(31L);
        var recipients = List.of(7L, 9L);
        when(messages.insert(any(SysMessage.class))).thenAnswer(call -> {
            SysMessage row = call.getArgument(0);
            row.setMessageId(41L);
            return 1;
        });
        try (var beans = mockStatic(SpringUtil.class); var push = mockStatic(PushHelper.class)) {
            beans.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(JsonMapper.builder().build());
            if (broadcast) {
                new RemoteMessageServiceImpl(service).publishAllPayload(remotePayload(payload));
                push.verify(() -> PushHelper.publishAll(org.mockito.ArgumentMatchers.<PushPayloadDTO>argThat(sent -> Long.valueOf(41L).equals(sent.getMessageId()))));
            } else {
                new RemoteMessageServiceImpl(service).publishMessagePayload(recipients, remotePayload(payload));
                push.verify(() -> PushHelper.publishMessage(eq(recipients), argThat(sent -> Long.valueOf(41L).equals(sent.getMessageId()))));
            }
            var saved = ArgumentCaptor.forClass(SysMessage.class);
            verify(messages).insert(saved.capture());
            assertEquals(31L, payload.getMessageId());
            assertEquals(category, saved.getValue().getCategory());
            assertEquals(title, saved.getValue().getTitle());
            assertEquals("正文", saved.getValue().getContent());
            assertEquals("/detail", saved.getValue().getPath());
            assertEquals(broadcast ? "0" : "7,9", saved.getValue().getSendUserIds());
        }
    }

    @ParameterizedTest
    @CsvSource({"llm,backend","message,llm","unknown,backend"})
    void transientMessagesBypassMessageBox(String type, String source) {
        var messages = mock(SysMessageMapper.class);
        var payload = PushPayloadDTO.of(type, source, "临时消息", null);
        assertSame(payload, new SysMessageServiceImpl(messages).storeAll(payload));
        verifyNoInteractions(messages);
    }
}
