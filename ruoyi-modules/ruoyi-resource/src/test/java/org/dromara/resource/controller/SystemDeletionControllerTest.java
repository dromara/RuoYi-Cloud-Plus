package org.dromara.resource.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.domain.PageResult;
import org.dromara.resource.domain.bo.SysOssConfigBo;
import org.dromara.resource.domain.vo.SysOssConfigVo;
import org.dromara.resource.service.ISysOssService;
import org.dromara.resource.service.ISysOssConfigService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemDeletionControllerTest {

    @Test
    void reportsFailureWhenValidatedOssDeletionFails() {
        var service = mock(ISysOssService.class);
        var ids = List.of(11L, 14L);
        when(service.deleteWithValidByIds(ids, true)).thenReturn(false);

        var response = new SysOssController(service).remove(new Long[]{11L, 14L});

        assertTrue(R.isError(response));
        verify(service).deleteWithValidByIds(ids, true);
    }

    @Test
    void reportsFailureWhenValidatedOssConfigDeletionFails() {
        var service = mock(ISysOssConfigService.class);
        var ids = List.of(11L, 14L);
        when(service.deleteWithValidByIds(ids, true)).thenReturn(false);

        var response = new SysOssConfigController(service).remove(new Long[]{11L, 14L});

        assertTrue(R.isError(response));
        verify(service).deleteWithValidByIds(ids, true);
    }
}
