package org.dromara.system.service.impl;

import org.dromara.common.core.constant.CacheNames;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.system.domain.SysDictType;
import org.dromara.system.domain.bo.SysDictTypeBo;
import org.dromara.system.mapper.SysDictDataMapper;
import org.dromara.system.mapper.SysDictTypeMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SysDictTypeServiceImplTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void creatingTypeInvalidatesCachedMissOnlyAfterInsert(int rows) {
        var types = mock(SysDictTypeMapper.class);
        var entries = mock(SysDictDataMapper.class);
        var bo = new SysDictTypeBo();
        bo.setDictType("new_status");
        var type = new SysDictType();
        type.setDictType("new_status");
        when(types.insert(type)).thenReturn(rows);
        var service = new SysDictTypeServiceImpl(types, entries);
        try (var spring = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class);
             var cache = mockStatic(CacheUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(bo, SysDictType.class)).thenReturn(type);
            if (rows == 0) {
                assertThrows(ServiceException.class, () -> service.insertDictType(bo));
                cache.verifyNoInteractions();
            } else {
                assertEquals(List.of(), service.insertDictType(bo));
                cache.verify(() -> CacheUtils.evict(CacheNames.SYS_DICT_TYPE, "new_status"));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void renamingTypeInvalidatesBothMetadataKeysOnlyAfterUpdate(int rows) {
        var types = mock(SysDictTypeMapper.class);
        var entries = mock(SysDictDataMapper.class, RETURNS_DEEP_STUBS);
        var original = new SysDictType();
        original.setDictType("old_status");
        var bo = new SysDictTypeBo();
        bo.setDictId(8L);
        bo.setDictType("new_status");
        var type = new SysDictType();
        type.setDictId(8L);
        type.setDictType("new_status");
        when(types.selectById(8L)).thenReturn(original);
        when(types.updateById(type)).thenReturn(rows);
        var update = entries.lambda();
        when(update.set(any(), eq("new_status"))).thenReturn(update);
        when(update.eq(any(), eq("old_status"))).thenReturn(update);
        var service = new SysDictTypeServiceImpl(types, entries);
        try (var spring = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class);
             var cache = mockStatic(CacheUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(bo, SysDictType.class)).thenReturn(type);
            if (rows == 0) {
                assertThrows(ServiceException.class, () -> service.updateDictType(bo));
                verify(update, never()).update();
                cache.verifyNoInteractions();
            } else {
                service.updateDictType(bo);
                verify(update).update();
                cache.verify(() -> CacheUtils.evict(CacheNames.SYS_DICT, "old_status"));
                cache.verify(() -> CacheUtils.evict(CacheNames.SYS_DICT_TYPE, "new_status"));
            }
        }
    }

    @Test
    void updatingDeletedTypeReportsBusinessFailureBeforeTouchingEntries() {
        var types = mock(SysDictTypeMapper.class);
        var entries = mock(SysDictDataMapper.class, RETURNS_DEEP_STUBS);
        var bo = new SysDictTypeBo();
        bo.setDictId(8L);
        var type = new SysDictType();
        type.setDictId(8L);
        try (var spring = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(bo, SysDictType.class)).thenReturn(type);
            assertThrows(ServiceException.class, () -> new SysDictTypeServiceImpl(types, entries).updateDictType(bo));
            verifyNoInteractions(entries);
        }
    }

    @Test
    void editingTypeDescriptionLeavesEntryRowsUntouched() {
        var types = mock(SysDictTypeMapper.class);
        var entries = mock(SysDictDataMapper.class, RETURNS_DEEP_STUBS);
        var bo = new SysDictTypeBo();
        bo.setDictId(8L);
        bo.setDictType("status");
        bo.setDictName("订单状态");
        var type = new SysDictType();
        type.setDictId(8L);
        type.setDictType("status");
        type.setDictName(bo.getDictName());
        var original = new SysDictType();
        original.setDictType("status");
        when(types.selectById(8L)).thenReturn(original);
        when(types.updateById(type)).thenReturn(1);
        var update = entries.lambda();
        when(update.set(any(), any())).thenReturn(update);
        when(update.eq(any(), any())).thenReturn(update);
        clearInvocations(entries, update);
        try (var spring = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class);
             var cache = mockStatic(CacheUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(bo, SysDictType.class)).thenReturn(type);
            new SysDictTypeServiceImpl(types, entries).updateDictType(bo);
            verify(entries).selectDictDataByType("status");
            verify(update, never()).update();
        }
    }
}
