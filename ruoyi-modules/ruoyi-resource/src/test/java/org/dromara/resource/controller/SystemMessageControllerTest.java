package org.dromara.resource.controller;

import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.resource.domain.vo.SysMessageBoxVo;
import org.dromara.resource.service.ISysMessageService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class SystemMessageControllerTest {

    @Test
    void returnsTheSignedInUsersMessageBox() {
        var messages = mock(ISysMessageService.class);
        var inbox = new SysMessageBoxVo();
        when(messages.queryMessageBox(21L)).thenReturn(inbox);

        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(21L);
            assertSame(inbox, new SysMessageController(messages).getBox().getData());
        }
    }
}
