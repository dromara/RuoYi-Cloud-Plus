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

class ResourceLookupContractTest {

    @Test
    void returnsOssConfigDetailsAndPreservesFilteredPageTotal() {
        var service = mock(ISysOssConfigService.class);
        var controller = new SysOssConfigController(service);
        var row = new SysOssConfigVo();
        when(service.queryById(42L)).thenReturn(row);
        var criteria = new SysOssConfigBo();
        var paging = new PageQuery(5, 3);
        when(service.queryPageList(criteria, paging)).thenReturn(PageResult.build(List.of(row), 18L));
        assertSame(row, controller.getInfo(42L).getData());
        var page = controller.list(criteria, paging).getData();
        assertEquals(List.of(row), page.getRows());
        assertEquals(18L, page.getTotal());
    }
}
