package org.dromara.common.core.service.impl;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.dromara.system.api.RemoteDictService;
import org.dromara.system.api.domain.vo.RemoteDictDataVo;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DictServiceImplTest {

    @ParameterizedTest(name = "按字面分隔符 {0} 双向转换，保留输入顺序")
    @ValueSource(strings = {"|", ".", "[+]", ","})
    void shouldTranslateMultipleEntriesUsingLiteralSeparator(String separator) {
        var service = new DictServiceImpl();
        var remoteService = mock(RemoteDictService.class);
        ReflectionTestUtils.setField(service, "remoteDictService", remoteService);
        ReflectionTestUtils.setField(service, "ceffeine", Caffeine.newBuilder().build());
        var pending = new RemoteDictDataVo();
        pending.setDictValue("pending");
        pending.setDictLabel("待处理");
        var done = new RemoteDictDataVo();
        done.setDictValue("done");
        done.setDictLabel("已完成");
        when(remoteService.selectDictDataByType("status")).thenReturn(List.of(pending, done));

        String values = "done" + separator + "pending";
        String labels = "已完成" + separator + "待处理";
        assertAll(
            () -> assertEquals(labels, service.getDictLabel("status", values, separator)),
            () -> assertEquals(values, service.getDictValue("status", labels, separator))
        );
    }
}
