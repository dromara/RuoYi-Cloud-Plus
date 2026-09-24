package org.dromara.common.core.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import org.dromara.system.api.RemoteDictService;
import org.dromara.system.api.RemotePermissionService;
import org.dromara.system.api.domain.vo.RemoteDictDataVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("common-service-impl 功能单元测试")
class ServiceImplFunctionTest {

    private RemoteDictService remoteDictService;

    private DictServiceImpl dictService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        remoteDictService = mock(RemoteDictService.class);
        // 用本地 Map 模拟 caffeine 键级缓存：首次回源 mappingFunction，之后命中缓存值
        Map<Object, Object> store = new HashMap<>();
        Cache<Object, Object> caffeine = mock(Cache.class);
        when(caffeine.get(any(), any())).thenAnswer(invocation ->
            store.computeIfAbsent((Object) invocation.getArgument(0),
                key -> ((Function<Object, Object>) invocation.getArgument(1)).apply(key)));

        dictService = new DictServiceImpl();
        ReflectionTestUtils.setField(dictService, "ceffeine", caffeine);
        ReflectionTestUtils.setField(dictService, "remoteDictService", remoteDictService);
    }

    /**
     * 验证字典标签翻译支持单值、多值分隔与未匹配回退空串，且远程结果被本地缓存。
     */
    @Test
    @DisplayName("翻译字典标签")
    void shouldTranslateDictLabels() {
        when(remoteDictService.selectDictDataByType("sys_user_sex")).thenReturn(new ArrayList<>(List.of(
            dict("0", "男"), dict("1", "女"))));

        assertEquals("男", dictService.getDictLabel("sys_user_sex", "0", ","));
        assertEquals("男,女", dictService.getDictLabel("sys_user_sex", "0,1", ","));
        assertEquals("", dictService.getDictLabel("sys_user_sex", "9", ","));

        // 三次查询只回源一次，其余命中本地缓存
        verify(remoteDictService, times(1)).selectDictDataByType("sys_user_sex");
    }

    /**
     * 验证字典值翻译按标签反向查找，多标签用分隔符拼接。
     */
    @Test
    @DisplayName("翻译字典值")
    void shouldTranslateDictValues() {
        when(remoteDictService.selectDictDataByType("sys_user_sex")).thenReturn(new ArrayList<>(List.of(
            dict("0", "男"), dict("1", "女"))));

        assertEquals("0", dictService.getDictValue("sys_user_sex", "男", ","));
        assertEquals("0,1", dictService.getDictValue("sys_user_sex", "男,女", ","));
        assertEquals("", dictService.getDictValue("sys_user_sex", "未知", ","));
    }

    /**
     * 验证全量字典按远程返回顺序保序输出，空字典返回空 Map。
     */
    @Test
    @DisplayName("获取全量字典映射")
    void shouldReturnAllDictInOrder() {
        when(remoteDictService.selectDictDataByType("ordered")).thenReturn(new ArrayList<>(List.of(
            dict("0", "男"), dict("1", "女"))));
        when(remoteDictService.selectDictDataByType("empty")).thenReturn(new ArrayList<>());

        Map<String, String> all = dictService.getAllDictByDictType("ordered");
        assertEquals(List.of("0", "1"), List.copyOf(all.keySet()));
        assertEquals("男", all.get("0"));

        assertTrue(dictService.getAllDictByDictType("empty").isEmpty());
    }

    /**
     * 验证权限服务将角色与菜单权限查询委托给远程服务。
     */
    @Test
    @DisplayName("委托远程权限查询")
    void shouldDelegatePermissionQueries() {
        RemotePermissionService remotePermissionService = mock(RemotePermissionService.class);
        when(remotePermissionService.getRolePermission(1L)).thenReturn(Set.of("admin"));
        when(remotePermissionService.getMenuPermission(1L)).thenReturn(Set.of("system:user:list"));

        PermissionServiceImpl service = new PermissionServiceImpl();
        ReflectionTestUtils.setField(service, "remotePermissionService", remotePermissionService);

        assertEquals(Set.of("admin"), service.getRolePermission(1L));
        assertEquals(Set.of("system:user:list"), service.getMenuPermission(1L));
        verify(remotePermissionService).getRolePermission(1L);
        verify(remotePermissionService).getMenuPermission(1L);
    }

    private static RemoteDictDataVo dict(String value, String label) {
        RemoteDictDataVo vo = new RemoteDictDataVo();
        vo.setDictValue(value);
        vo.setDictLabel(label);
        return vo;
    }

}
