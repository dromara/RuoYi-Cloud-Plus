package org.dromara.workflow.controller;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.domain.bo.FlowEventWaitBo;
import org.dromara.workflow.domain.bo.FlowTimerTriggerBo;
import org.dromara.workflow.domain.vo.FlowEventWaitVo;
import org.dromara.workflow.service.IFlwEventWaitService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlwEventWaitControllerTest {
    private final IFlwEventWaitService events = mock(IFlwEventWaitService.class);
    private final FlwEventWaitController controller = new FlwEventWaitController(events);

    @Test
    void listAndSummaryUseTheSameFilterAndDetailUsesTheRequestedId() {
        var filter = new FlowEventWaitBo();
        var record = new FlowEventWaitVo();
        when(events.queryPageList(filter)).thenReturn(PageResult.build(List.of(record), 9L));
        when(events.summary(filter)).thenReturn(Map.of("waiting", 9L));
        when(events.queryById(7L)).thenReturn(record);

        assertEquals(9L, controller.list(filter).getData().getTotal());
        assertEquals(Map.of("waiting", 9L), controller.summary(filter).getData());
        assertSame(record, controller.getInfo(7L).getData());
    }

    @Test
    void manualTriggerReturnsInstanceIdAndRevivePropagatesRejection() {
        var request = new FlowTimerTriggerBo();
        when(events.manualTrigger(request)).thenReturn(12L);
        doThrow(new ServiceException("cannot revive")).when(events).revive(8L);

        assertEquals(12L, controller.trigger(request).getData());
        assertTrue(R.isSuccess(controller.revive(7L)));
        verify(events).revive(7L);
        assertThrows(ServiceException.class, () -> controller.revive(8L));
    }
}
