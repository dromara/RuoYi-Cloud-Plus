package org.dromara.system.controller.system;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.service.DictService;
import org.dromara.resource.api.RemoteMessageService;
import org.dromara.resource.api.domain.RemotePushPayLoad;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import org.dromara.system.domain.bo.SysNoticeBo;
import org.dromara.system.service.ISysNoticeService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemMessageControllerTest {

    @Test
    void broadcastsPersistedNoticeAndItsDetailLink() {
        var notices = mock(ISysNoticeService.class);
        var dictionary = mock(DictService.class);
        var broadcast = mock(RemoteMessageService.class);
        var notice = new SysNoticeBo();
        notice.setNoticeTitle("维护");
        notice.setNoticeType("2");
        notice.setNoticeContent("周末升级");
        notice.setStatus("0");
        when(notices.insertNotice(notice)).thenAnswer(invocation -> {
            notice.setNoticeId(31L);
            return 1;
        });
        when(dictionary.getDictLabel("sys_notice_type", "2")).thenReturn("公告");

        var controller = new SysNoticeController(notices, dictionary);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "remoteMessageService", broadcast);
        var response = controller.add(notice);

        assertTrue(R.isSuccess(response));
        var payload = ArgumentCaptor.forClass(RemotePushPayLoad.class);
        verify(broadcast).publishAllPayload(payload.capture());
        assertEquals("[公告] 维护", payload.getValue().getMessage());
        assertEquals("/system/notice?noticeId=31", payload.getValue().getPath());
        assertEquals(Map.of("noticeId", 31L, "noticeType", "2", "noticeTypeLabel", "公告",
            "noticeTitle", "维护", "noticeContent", "周末升级", "status", "0"), payload.getValue().getData());
    }

    @Test
    void failedNoticeInsertSuppressesBroadcast() {
        var notices = mock(ISysNoticeService.class);
        var dictionary = mock(DictService.class);
        var broadcast = mock(RemoteMessageService.class);
        var notice = new SysNoticeBo();
        when(notices.insertNotice(notice)).thenReturn(0);

        var controller = new SysNoticeController(notices, dictionary);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "remoteMessageService", broadcast);
        var response = controller.add(notice);

        assertTrue(R.isError(response));
        verify(notices).insertNotice(notice);
        verifyNoInteractions(broadcast);
    }

}
