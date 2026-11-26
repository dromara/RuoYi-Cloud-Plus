package org.dromara.workflow.service.impl;

import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.api.domain.RemoteStartProcess;
import org.dromara.workflow.api.domain.RemoteStartProcessReturn;
import org.dromara.workflow.common.enums.MessageTypeEnum;
import org.dromara.workflow.domain.bo.CompleteTaskBo;
import org.dromara.workflow.domain.bo.StartProcessBo;
import org.dromara.workflow.service.IFlwInstanceService;
import org.dromara.workflow.service.IFlwTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowFacadeTest {
    private final IFlwTaskService tasks = mock(IFlwTaskService.class);
    private final IFlwInstanceService instances = mock(IFlwInstanceService.class);
    private final WorkflowServiceImpl workflow = new WorkflowServiceImpl(instances, null, tasks);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void completesReturnedTaskWithOriginalHandlerAndVariables(boolean outcome) {
        RemoteStartProcess request = new RemoteStartProcess();
        request.setBusinessId("leave-1");
        request.setFlowCode("leave");
        request.setHandler("7");
        request.setVariables(Map.of("days", 3));
        request.getBizExt().setBusinessTitle("年假");
        var started = new RemoteStartProcessReturn();
        started.setProcessInstanceId(9L);
        started.setTaskId(17L);
        when(tasks.startWorkFlow(any())).thenReturn(started);
        when(tasks.completeTask(any())).thenReturn(outcome);
        assertEquals(outcome, workflow.startCompleteTask(request));
        var start = ArgumentCaptor.forClass(StartProcessBo.class);
        var complete = ArgumentCaptor.forClass(CompleteTaskBo.class);
        verify(tasks).startWorkFlow(start.capture());
        verify(tasks).completeTask(complete.capture());
        assertEquals("leave-1", start.getValue().getBusinessId());
        assertEquals("leave", start.getValue().getFlowCode());
        assertEquals("年假", start.getValue().getBizExt().getBusinessTitle());
        assertEquals("7", start.getValue().getHandler());
        assertEquals(Map.of("days", 3), start.getValue().getVariables());
        assertEquals(17L, complete.getValue().getTaskId());
        assertEquals("7", complete.getValue().getHandler());
        assertEquals(List.of(MessageTypeEnum.SYSTEM_MESSAGE.getCode()), complete.getValue().getMessageType());
        assertEquals(3, complete.getValue().getVariables().get("days"));
    }

    @Test
    void failedStartNeverCompletesTask() {
        when(tasks.startWorkFlow(any())).thenThrow(new IllegalStateException("start failed"));
        assertThrows(IllegalStateException.class, () -> workflow.startCompleteTask(new RemoteStartProcess()));
        verify(tasks, never()).completeTask(any());
    }

    @Test
    void completionDtoPreservesReadOnlyBusinessVariables() {
        var dto = new org.dromara.workflow.api.domain.RemoteCompleteTask();
        dto.setTaskId(24L);
        dto.setVariables(Map.of("days", 2));
        when(tasks.completeTask(any())).thenReturn(true);
        assertTrue(workflow.completeTask(dto));
        var request = ArgumentCaptor.forClass(CompleteTaskBo.class);
        verify(tasks).completeTask(request.capture());
        assertEquals(24L, request.getValue().getTaskId());
        assertEquals(Map.of("days", 2), request.getValue().getVariables());
    }

    @Test
    void backgroundCompletionCarriesSystemFlag() {
        workflow.completeTask(11L, "自动办理");
        var request = ArgumentCaptor.forClass(CompleteTaskBo.class);
        verify(tasks).completeTask(request.capture());
        assertEquals(11L, request.getValue().getTaskId());
        assertEquals("自动办理", request.getValue().getMessage());
        assertEquals(true, request.getValue().getVariables().get("ignore"));
    }

    @Test
    void returnsBusinessStatusAndDistinguishesMissingId() {
        assertEquals("", workflow.getBusinessStatus("42"));
        assertEquals("", workflow.getBusinessStatusByTaskId(7L));
        assertNull(workflow.getInstanceIdByBusinessId("42"));
        FlowInstance instance = new FlowInstance();
        instance.setId(19L);
        instance.setFlowStatus("finish");
        when(instances.selectInstByBusinessId("42")).thenReturn(instance);
        when(instances.selectByTaskId(7L)).thenReturn(instance);
        assertEquals("finish", workflow.getBusinessStatus("42"));
        assertEquals("finish", workflow.getBusinessStatusByTaskId(7L));
        assertEquals(19L, workflow.getInstanceIdByBusinessId("42"));
    }
}
