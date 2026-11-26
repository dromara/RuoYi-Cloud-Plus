package org.dromara.workflow.dubbo;

import org.dromara.workflow.api.domain.*;
import org.dromara.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RemoteWorkflowServiceTest {

    private final WorkflowService workflow = mock(WorkflowService.class);
    private final RemoteWorkflowServiceImpl remote = new RemoteWorkflowServiceImpl(workflow);

    @Test
    void routesInstanceQueriesAndUpdates() {
        var variables = Map.<String, Object>of("approved", true);
        when(workflow.getBusinessStatus("order")).thenReturn("waiting");
        when(workflow.getBusinessStatusByTaskId(42L)).thenReturn("finish");
        when(workflow.getInstanceIdByBusinessId("order")).thenReturn(7L);
        when(workflow.instanceVariable(7L)).thenReturn(variables);
        when(workflow.deleteInstance(List.of("order"))).thenReturn(true);

        remote.setVariable(7L, variables);

        verify(workflow).setVariable(7L, variables);
        assertEquals(variables, remote.instanceVariable(7L));
        assertEquals("waiting", remote.getBusinessStatus("order"));
        assertEquals("finish", remote.getBusinessStatusByTaskId(42L));
        assertEquals(7L, remote.getInstanceIdByBusinessId("order"));
        assertTrue(remote.deleteInstance(List.of("order")));
    }

    @Test
    void routesWorkflowStartAndBothCompletionContracts() {
        var start = new RemoteStartProcess();
        var started = new RemoteStartProcessReturn();
        var completion = new RemoteCompleteTask();
        when(workflow.startWorkFlow(start)).thenReturn(started);
        when(workflow.startCompleteTask(start)).thenReturn(true);
        when(workflow.completeTask(completion)).thenReturn(true);
        when(workflow.completeTask(42L, "done")).thenReturn(false);

        assertSame(started, remote.startWorkFlow(start));
        assertTrue(remote.startCompleteTask(start));
        assertTrue(remote.completeTask(completion));
        assertFalse(remote.completeTask(42L, "done"));
        verify(workflow).completeTask(42L, "done");
    }

    @Test
    void routesEventCommandsAndPropagatesEngineFailure() {
        var message = new StartMessageEventDTO();
        var timer = new StartTimerEventDTO();
        var trigger = new TriggerMessageEventDTO();
        var failure = new IllegalStateException("workflow unavailable");
        when(workflow.startByMessageEvent(message)).thenReturn(1L);
        when(workflow.startByTimerEvent(timer)).thenReturn(2L);
        when(workflow.triggerMessageEvent(trigger)).thenReturn(3L);
        when(workflow.throwInstanceErrorEvent(7L, "TIMEOUT", "expired")).thenReturn(true);
        when(workflow.throwTaskErrorEvent(42L, "TIMEOUT", "expired")).thenReturn(false);
        when(workflow.throwTaskErrorEvent(99L, "TIMEOUT", "expired")).thenThrow(failure);

        assertEquals(1L, remote.startByMessageEvent(message));
        assertEquals(2L, remote.startByTimerEvent(timer));
        assertEquals(3L, remote.triggerMessageEvent(trigger));
        assertTrue(remote.throwInstanceErrorEvent(7L, "TIMEOUT", "expired"));
        assertFalse(remote.throwTaskErrorEvent(42L, "TIMEOUT", "expired"));
        assertSame(failure, assertThrows(IllegalStateException.class,
            () -> remote.throwTaskErrorEvent(99L, "TIMEOUT", "expired")));
    }
}
