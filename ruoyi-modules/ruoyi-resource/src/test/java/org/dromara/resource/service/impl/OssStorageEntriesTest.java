package org.dromara.resource.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.resource.domain.SysOss;
import org.dromara.resource.domain.bo.SysOssBo;
import org.dromara.resource.domain.bo.SysOssConfigBo;
import org.dromara.resource.domain.vo.SysOssConfigVo;
import org.dromara.resource.domain.vo.SysOssVo;
import org.dromara.resource.mapper.SysOssConfigMapper;
import org.dromara.resource.mapper.SysOssMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OssStorageEntriesTest {
    @Test
    void fileLookupSkipsMissingIdsAndKeepsStoredUrlWhenStorageIsUnavailable() {
        var mapper = mock(SysOssMapper.class);
        var files = new SysOssServiceImpl(mapper);
        var stored = new SysOssVo();
        stored.setUrl("/files/document");
        stored.setOssId(7L);
        when(mapper.selectVoById(7L)).thenReturn(stored);

        // Run suppliers in the test thread so the simulated OSS outage applies to each lookup.
        try (var spring = mockStatic(SpringUtils.class);
             var threads = mockStatic(org.dromara.common.core.utils.ThreadUtils.class);
             var storage = mockStatic(OssFactory.class)) {
            spring.when(() -> SpringUtils.getAopProxy(files)).thenReturn(files);
            threads.when(() -> org.dromara.common.core.utils.ThreadUtils.virtualSubmitAll(anyCollection()))
                .thenAnswer(call -> {
                    java.util.Collection<java.util.function.Supplier<SysOssVo>> tasks = call.getArgument(0);
                    var rows = new java.util.ArrayList<SysOssVo>();
                    tasks.forEach(task -> rows.add(task.get()));
                    return rows;
                });
            storage.when(() -> OssFactory.instance((String) null)).thenThrow(new IllegalStateException("storage unavailable"));

            assertEquals(List.of(stored), files.listByIds(List.of(8L, 7L)));
            assertSame(stored, files.getById(7L));
        }
    }

    @Test
    void preservesFileAndConfigurationPageTotalsAndReturnsGeneratedFileId() {
        var files = mock(SysOssMapper.class);
        var configs = mock(SysOssConfigMapper.class);
        var configuration = new SysOssConfigVo();
        var data = new Page<SysOssConfigVo>(1, 10, 12);
        data.setRecords(List.of(configuration));
        when(configs.selectVoById(7L)).thenReturn(configuration);
        when(configs.selectVoPage(any(), any())).thenReturn(data);
        when(files.selectVoPage(any(), any())).thenReturn(new Page<SysOssVo>(1, 10, 6));
        when(files.insert(any(SysOss.class))).thenAnswer(call -> {
            SysOss file = call.getArgument(0);
            file.setOssId(42L);
            return 1;
        });
        var storage = new SysOssServiceImpl(files);
        var configService = new SysOssConfigServiceImpl(configs);
        var input = new SysOssBo();

        assertEquals(6L, storage.queryPageList(input, new PageQuery()).getTotal());
        assertSame(configuration, configService.queryById(7L));
        assertEquals(List.of(configuration), configService.queryPageList(new SysOssConfigBo(), new PageQuery()).getRows());
        assertTrue(storage.insertByBo(input));
        assertEquals(42L, input.getOssId());
    }
}
