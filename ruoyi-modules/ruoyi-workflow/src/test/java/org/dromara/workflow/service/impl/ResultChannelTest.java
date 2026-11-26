package org.dromara.workflow.service.impl;

import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.utils.ThreadUtils;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.resource.api.RemoteMailService;
import org.springframework.test.util.ReflectionTestUtils;
import org.dromara.resource.api.RemoteMessageService;
import org.dromara.system.api.domain.vo.RemoteUserVo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.dromara.workflow.service.IFlwTaskService;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

class ResultChannelTest {
    @Test
    void parallelTasksNotifySharedAssigneeOnceEvenWithRepeatedChannelSelection() {
        var messages = mock(RemoteMessageService.class);
        var service = new FlwCommonServiceImpl();
        ReflectionTestUtils.setField(service, "remoteMessageService", messages);
        var tasks = mock(IFlwTaskService.class);
        var first = new FlowTask();
        first.setId(11L);
        var second = new FlowTask();
        second.setId(12L);
        var assignee = new RemoteUserVo();
        assignee.setUserId(7L);
        when(tasks.selectByInstId(31L)).thenReturn(List.of(first, second));
        when(tasks.currentTaskAllUser(List.of(11L, 12L))).thenReturn(List.of(assignee, assignee));
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(IFlwTaskService.class)).thenReturn(tasks);
            service.sendMessage("请假", 31L, List.of("1", "1"), "请复核");
        }
        verify(messages).publishMessagePayload(eq(List.of(7L)), argThat(payload ->
            "/task/taskWaiting".equals(payload.getPath()) && "请复核".equals(payload.getMessage())));
        verifyNoMoreInteractions(messages);
    }

    @Test
    void delayedNotificationForInstanceWithNoRemainingTasksDoesNotReachFormerAssignees() {
        var messages = mock(RemoteMessageService.class);
        var service = new FlwCommonServiceImpl();
        ReflectionTestUtils.setField(service, "remoteMessageService", messages);
        var tasks = mock(IFlwTaskService.class);
        when(tasks.selectByInstId(31L)).thenReturn(List.of());
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(IFlwTaskService.class)).thenReturn(tasks);
            service.sendMessage("请假", 31L, List.of("1"), "请复核");
        }
        verifyNoInteractions(messages);
    }

    @ParameterizedTest
    @ValueSource(strings = {"reviewer@example.org", " "})
    void resultUsesDocumentRouteAndOnlySendsEmailToAvailableAddresses(String address) {
        var messages = mock(RemoteMessageService.class);
        var service = new FlwCommonServiceImpl();
        ReflectionTestUtils.setField(service, "remoteMessageService", messages);
        var user = new RemoteUserVo();
        user.setUserId(7L);
        user.setEmail(address);
        var mail = mock(RemoteMailService.class);
        ReflectionTestUtils.setField(service, "remoteMailService", mail);
        try (var threads = mockStatic(ThreadUtils.class)) {
            threads.when(() -> ThreadUtils.virtualInvokeAll(any(Runnable[].class))).thenAnswer(call -> {
                for (Runnable task : (Runnable[]) call.getRawArguments()[0]) {
                    task.run();
                }
                return null;
            });
            service.sendResultMessage("请假", BusinessStatusEnum.FINISH, List.of(), List.of(user));
            service.sendResultMessage("请假", BusinessStatusEnum.FINISH, List.of("1"), List.of());
            threads.verifyNoInteractions();
            service.sendResultMessage("请假", BusinessStatusEnum.FINISH, List.of("1", "2"), List.of(user, user));
            verify(messages).publishMessagePayload(eq(List.of(7L)), argThat(payload ->
                "/task/myDocument".equals(payload.getPath()) && payload.getMessage().contains("请假")
                    && payload.getMessage().contains("已完成")));
            if (address.isBlank()) {
                verifyNoInteractions(mail);
            } else {
                verify(mail).send(anyString(), anyString(), anyString());
            }
        }
    }
}
