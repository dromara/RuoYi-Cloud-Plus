package org.dromara.resource.dubbo;

import org.dromara.common.mail.core.MailBuilder;
import org.dromara.resource.service.ISysOssService;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RemoteResourceServiceTest {

    @Test
    void fileRpcParsesIdListAndConvertsFileMetadata() {
        var oss = mock(ISysOssService.class);
        var stored = new org.dromara.resource.domain.vo.SysOssVo();
        stored.setOssId(9L);
        var file = new org.dromara.resource.api.domain.RemoteFile();
        file.setOssId(9L);
        when(oss.listByIds(List.of(9L, 7L))).thenReturn(List.of(stored));
        try (var beans = mockStatic(cn.hutool.extra.spring.SpringUtil.class)) {
            var converter = mock(io.github.linpeilie.Converter.class);
            beans.when(() -> cn.hutool.extra.spring.SpringUtil.getBean(io.github.linpeilie.Converter.class)).thenReturn(converter);
            org.dromara.common.core.utils.MapstructUtils.convert(List.of(), org.dromara.resource.api.domain.RemoteFile.class);
            try (var mapping = mockStatic(org.dromara.common.core.utils.MapstructUtils.class)) {
                mapping.when(() -> org.dromara.common.core.utils.MapstructUtils.convert(List.of(stored),
                    org.dromara.resource.api.domain.RemoteFile.class)).thenReturn(List.of(file));
                assertEquals(List.of(file), new RemoteFileServiceImpl(oss).selectByIds("9,7"));
                verify(oss).listByIds(List.of(9L, 7L));
            }
        }
    }

    @Test
    void textMessageRpcPreservesRecipientScopeAndUsesStoredPayload() {
        var messages = mock(org.dromara.resource.service.ISysMessageService.class);
        var service = new RemoteMessageServiceImpl(messages);
        var pushed = new org.dromara.common.push.dto.PushPayloadDTO();
        when(messages.storeUsers(eq(List.of(42L)), any())).thenReturn(pushed);
        when(messages.storeAll(any())).thenReturn(pushed);
        try (var push = mockStatic(org.dromara.common.push.helper.PushHelper.class)) {
            service.publishMessage(List.of(42L), "personal");
            service.publishAll("all users");
            verify(messages).storeUsers(eq(List.of(42L)), argThat(payload -> "personal".equals(payload.getMessage())));
            verify(messages).storeAll(argThat(payload -> "all users".equals(payload.getMessage())));
            push.verify(() -> org.dromara.common.push.helper.PushHelper.publishMessage(List.of(42L), pushed));
            push.verify(() -> org.dromara.common.push.helper.PushHelper.publishAll(pushed));
        }
    }

    @Test
    void emptyFileLookupsSkipOssAndUrlLookupDelegates() {
        var oss = mock(ISysOssService.class);
        var files = new RemoteFileServiceImpl(oss);

        assertNull(files.selectById(null));
        assertTrue(files.selectListByIds(null).isEmpty());
        assertTrue(files.selectListByIds(List.of()).isEmpty());
        verifyNoInteractions(oss);

        when(oss.selectUrlByIds("3,5")).thenReturn("/files/a,/files/b");
        assertEquals("/files/a,/files/b", files.selectUrlByIds("3,5"));
    }

    @Test
    void remoteMailForwardsRecipientSubjectAndBody() {
        var mail = mock(MailBuilder.class, RETURNS_SELF);
        try (var builders = mockStatic(MailBuilder.class)) {
            builders.when(MailBuilder::of).thenReturn(mail);

            new RemoteMailServiceImpl().send("reviewer@example.org", "Review", "Decision ready");

            var order = inOrder(mail);
            order.verify(mail).to("reviewer@example.org");
            order.verify(mail).subject("Review");
            order.verify(mail).text("Decision ready");
            order.verify(mail).send();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void remoteSmsPreservesProviderOutcomeAndTemplateArguments(boolean success) {
        var provider = mock(SmsBlend.class);
        var response = mock(SmsResponse.class);
        var parameters = new LinkedHashMap<String, String>();
        parameters.put("name", "Alex");
        when(response.isSuccess()).thenReturn(success);
        when(response.getData()).thenReturn("provider-result");
        when(response.getConfigId()).thenReturn("config1");
        when(provider.sendMessage("15500000001", "notice", parameters)).thenReturn(response);
        try (var factory = mockStatic(SmsFactory.class)) {
            factory.when(() -> SmsFactory.getSmsBlend("config1")).thenReturn(provider);

            var result = new RemoteSmsServiceImpl().sendMessage("15500000001", "notice", parameters);

            assertEquals(success, result.getSuccess());
            assertEquals("provider-result", result.getResponse());
            assertEquals("config1", result.getConfigId());
            verify(provider).sendMessage("15500000001", "notice", parameters);
        }
    }

    @Test
    void remoteSmsRoutesSingleAndBatchMessagesWithTheirDeliveryOptions() {
        var provider = mock(SmsBlend.class);
        var response = mock(SmsResponse.class);
        var service = new RemoteSmsServiceImpl();
        String phone = "15500000001";
        var phones = List.of(phone, "15500000002");
        var parameters = new LinkedHashMap<String, String>();
        parameters.put("order", "42");
        when(response.getData()).thenReturn("accepted");
        when(provider.sendMessage(phone, "notice")).thenReturn(response);
        when(provider.sendMessage(phone, parameters)).thenReturn(response);
        when(provider.massTexting(phones, "notice")).thenReturn(response);
        when(provider.massTexting(phones, "template", parameters)).thenReturn(response);
        try (var factory = mockStatic(SmsFactory.class)) {
            factory.when(() -> SmsFactory.getSmsBlend("config1")).thenReturn(provider);
            assertEquals("accepted", service.sendMessage(phone, "notice").getResponse());
            assertEquals("accepted", service.sendMessage(phone, parameters).getResponse());
            assertEquals("accepted", service.messageTexting(phones, "notice").getResponse());
            assertEquals("accepted", service.messageTexting(phones, "template", parameters).getResponse());

            service.sendMessageAsync(phone, "notice");
            service.sendMessageAsync(phone, "template", parameters);
            service.delayMessage(phone, "notice", 500L);
            service.delayMessage(phone, "template", parameters, 500L);
            service.delayMessageTexting(phones, "notice", 500L);
            service.delayMessageTexting(phones, "template", parameters, 500L);
            service.addBlacklist(phone);
            service.addBlacklist(phones);
            service.removeBlacklist(phone);
            service.removeBlacklist(phones);
        }

        verify(provider).sendMessageAsync(phone, "notice");
        verify(provider).sendMessageAsync(phone, "template", parameters);
        verify(provider).delayedMessage(phone, "notice", 500L);
        verify(provider).delayedMessage(phone, "template", parameters, 500L);
        verify(provider).delayMassTexting(phones, "notice", 500L);
        verify(provider).delayMassTexting(phones, "template", parameters, 500L);
        verify(provider).joinInBlacklist(phone);
        verify(provider).batchJoinBlacklist(phones);
        verify(provider).removeFromBlacklist(phone);
        verify(provider).batchRemovalFromBlacklist(phones);
    }
}
