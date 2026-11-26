package org.dromara.gen.controller;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.gen.domain.GenTable;
import org.dromara.gen.domain.GenTableColumn;
import org.dromara.gen.service.IGenTableService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GenControllerTest {
    private final IGenTableService tables = mock(IGenTableService.class);
    private final GenController controller = new GenController(tables);

    @Test
    void returnsTablePagesColumnDetailsAndAvailableSources() {
        var criteria = new GenTable();
        var query = new PageQuery();
        var table = new GenTable();
        var column = new GenTableColumn();
        when(tables.selectPageGenTableList(criteria, query)).thenReturn(PageResult.build(List.of(table), 12L));
        when(tables.selectPageDbTableList(criteria, query)).thenReturn(PageResult.build(List.of(table), 20L));
        when(tables.selectGenTableById(7L)).thenReturn(table);
        when(tables.selectGenTableColumnListByTableId(7L)).thenReturn(List.of(column));

        assertEquals(12L, controller.genList(criteria, query).getData().getTotal());
        assertEquals(20L, controller.dataList(criteria, query).getData().getTotal());
        assertEquals(Map.of("info", table, "rows", List.of(column)), controller.getInfo(7L).getData());
        assertEquals(List.of(column), controller.columnList(7L).getData().getRows());
        try (var beans = mockStatic(cn.hutool.extra.spring.SpringUtil.class)) {
            var dataSource = mock(com.baomidou.dynamic.datasource.DynamicRoutingDataSource.class);
            beans.when(() -> cn.hutool.extra.spring.SpringUtil.getBean(com.baomidou.dynamic.datasource.DynamicRoutingDataSource.class))
                .thenReturn(dataSource);
            when(dataSource.getDataSources()).thenReturn(Map.of("master", mock(javax.sql.DataSource.class)));
            assertEquals(List.of("master"), controller.getCurrentDataSourceNameList().getData());
        }
    }

    @Test
    void editsOnlyAfterValidationAndRoutesImportSyncAndDelete() {
        var table = new GenTable();
        var selected = List.of(table);
        when(tables.selectDbTableListByNames(new String[]{"invoice", "payment"}, "master")).thenReturn(selected);

        assertTrue(R.isSuccess(controller.importTableSave("invoice,payment", "master")));
        assertTrue(R.isSuccess(controller.editSave(table)));
        assertTrue(R.isSuccess(controller.synchDb(7L)));
        assertTrue(R.isSuccess(controller.remove(new Long[]{7L, 8L})));
        var operations = inOrder(tables);
        operations.verify(tables).selectDbTableListByNames(new String[]{"invoice", "payment"}, "master");
        operations.verify(tables).importGenTable(selected, "master");
        operations.verify(tables).validateEdit(table);
        operations.verify(tables).updateGenTable(table);
        operations.verify(tables).synchDb(7L);
        operations.verify(tables).deleteGenTableByIds(new Long[]{7L, 8L});
    }

    @Test
    void returnsPreviewAndWritesSingleAndBatchArchives() throws Exception {
        var preview = Map.of("invoice.java", "class Invoice {}");
        when(tables.previewCode(7L)).thenReturn(preview);
        byte[] archive = {1, 2, 3};
        when(tables.downloadCode(7L)).thenReturn(archive);
        when(tables.downloadCode(new String[]{"7", "8"})).thenReturn(archive);
        assertEquals(preview, controller.preview(7L).getData());
        var single = new MockHttpServletResponse();
        controller.download(single, 7L);
        assertArchive(single, archive);
        var batch = new MockHttpServletResponse();
        controller.batchGenCode(batch, "7,8");
        assertArchive(batch, archive);
    }

    private static void assertArchive(MockHttpServletResponse response, byte[] archive) {
        assertArrayEquals(archive, response.getContentAsByteArray());
        assertEquals("attachment; filename=\"ruoyi.zip\"", response.getHeader("Content-Disposition"));
        assertEquals(Integer.toString(archive.length), response.getHeader("Content-Length"));
        assertTrue(response.getContentType().startsWith("application/octet-stream"));
    }
}
