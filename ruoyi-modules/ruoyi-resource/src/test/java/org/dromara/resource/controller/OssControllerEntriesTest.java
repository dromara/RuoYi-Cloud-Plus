package org.dromara.resource.controller;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.resource.domain.bo.SysOssBo;
import org.dromara.resource.domain.bo.SysOssConfigBo;
import org.dromara.resource.domain.vo.SysOssVo;
import org.dromara.resource.service.ISysOssConfigService;
import org.dromara.resource.service.ISysOssService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OssControllerEntriesTest {
    @Test
    void exposesPagedFilesSelectedIdsAndTheDownloadResponse() throws Exception {
        var files = mock(ISysOssService.class);
        var controller = new SysOssController(files);
        var query = new SysOssBo();
        var paging = new PageQuery();
        var row = new SysOssVo();
        when(files.queryPageList(query, paging)).thenReturn(PageResult.build(List.of(row), 6L));
        when(files.listByIds(List.of(7L, 8L))).thenReturn(List.of(row));
        var download = ResponseEntity.ok().header("Content-Disposition", "attachment").body(new byte[]{42});
        when(files.download(7L)).thenReturn(download);

        assertEquals(6L, controller.list(query, paging).getData().getTotal());
        assertEquals(List.of(row), controller.listByIds(new Long[]{7L, 8L}).getData());
        assertSame(download, controller.download(7L));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void configurationWritesReturnTheOutcomeOfEachOperation(boolean saved) {
        var configs = mock(ISysOssConfigService.class);
        var controller = new SysOssConfigController(configs);
        var input = new SysOssConfigBo();
        when(configs.insertByBo(input)).thenReturn(saved);
        when(configs.updateByBo(input)).thenReturn(saved);
        when(configs.updateOssConfigStatus(input)).thenReturn(saved ? 1 : 0);

        assertEquals(saved, R.isSuccess(controller.add(input)));
        assertEquals(saved, R.isSuccess(controller.edit(input)));
        assertEquals(saved, R.isSuccess(controller.changeStatus(input)));
    }
}
