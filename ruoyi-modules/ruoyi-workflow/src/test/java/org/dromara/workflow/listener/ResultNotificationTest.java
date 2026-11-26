package org.dromara.workflow.listener;

import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.system.api.RemoteUserService;
import org.dromara.system.api.domain.vo.RemoteUserVo;
import org.dromara.workflow.common.enums.MessageTypeEnum;
import org.dromara.workflow.event.WorkflowResultMessageEvent;
import org.dromara.workflow.service.IFlwCommonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;
import static org.mockito.Mockito.*;

class ResultNotificationTest {
    private final RemoteUserService users = mock(RemoteUserService.class);
    private final IFlwCommonService messages = mock(IFlwCommonService.class);
    private final WorkflowSideEffectListener listener = new WorkflowSideEffectListener(null, messages);

    @org.junit.jupiter.api.BeforeEach
    void injectUserService() {
        org.springframework.test.util.ReflectionTestUtils.setField(listener, "remoteUserService", users);
    }

    @ParameterizedTest
    @EnumSource(value = BusinessStatusEnum.class, names = {"FINISH", "BACK"})
    void sendsResultToInitiatorUsingDefaultChannel(BusinessStatusEnum status) {
        RemoteUserVo initiator = new RemoteUserVo();
        initiator.setUserId(23L);
        when(users.selectListByIds(List.of(23L))).thenReturn(List.of(initiator));
        listener.handleResultMessage(new WorkflowResultMessageEvent("请假", status.getStatus(), "23", List.of()));
        verify(messages).sendResultMessage("请假", status, List.of(MessageTypeEnum.SYSTEM_MESSAGE.getCode()), List.of(initiator));
    }

    @Test
    void skipsIntermediateStatusAndMissingInitiator() {
        listener.handleResultMessage(new WorkflowResultMessageEvent("请假", BusinessStatusEnum.WAITING.getStatus(), "23", null));
        listener.handleResultMessage(new WorkflowResultMessageEvent("请假", BusinessStatusEnum.BACK.getStatus(), "invalid", null));
        verifyNoInteractions(users, messages);
        listener.handleResultMessage(new WorkflowResultMessageEvent("请假", BusinessStatusEnum.BACK.getStatus(), "23", null));
        verify(users).selectListByIds(List.of(23L));
        verifyNoInteractions(messages);
    }
}
