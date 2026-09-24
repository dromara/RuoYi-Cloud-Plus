package org.dromara.common.translation.core.impl;

import org.dromara.common.core.service.DictService;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.resource.api.RemoteFileService;
import org.dromara.resource.api.domain.RemoteFile;
import org.dromara.system.api.RemoteDeptService;
import org.dromara.system.api.RemoteUserService;
import org.dromara.system.api.domain.vo.RemoteUserVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@DisplayName("内置翻译实现单元测试")
class TranslationImplementationsTest {

    /**
     * 验证部门翻译将键统一转为字符串后委托远程服务，并按原始复合 ID 组装批量映射。
     */
    @Test
    @DisplayName("翻译部门名称")
    void shouldTranslateDepartmentNames() {
        RemoteDeptService service = mock(RemoteDeptService.class);
        when(service.selectDeptNameByIds("1")).thenReturn("研发部");
        when(service.selectDeptNameByIds("1,2")).thenReturn("研发部,财务部");
        when(service.selectDeptNamesByIds(Set.of(1L, 2L))).thenReturn(Map.of(1L, "研发部", 2L, "财务部"));
        DeptNameTranslationImpl translation = new DeptNameTranslationImpl(service);

        assertEquals("研发部", translation.translation(1L, null));
        assertEquals("研发部,财务部", translation.translation("1,2", null));
        assertEquals(Map.of(1L, "研发部", "2,1", "财务部,研发部"),
            translation.translationBatch(new LinkedHashSet<>(List.of(1L, "2,1")), null));
    }

    /**
     * 验证昵称翻译优先读取缓存、未命中回源远程服务，并忽略无法识别的键类型。
     */
    @Test
    @DisplayName("翻译用户昵称")
    void shouldTranslateNicknames() {
        RemoteUserService service = mock(RemoteUserService.class);
        when(service.selectNicknameById(1L)).thenReturn("管理员");
        when(service.selectNicknameById(2L)).thenReturn("访客");
        when(service.selectUserNicksByIds(Set.of(1L, 2L))).thenReturn(Map.of(1L, "管理员", 2L, "访客"));
        NicknameTranslationImpl translation = new NicknameTranslationImpl(service);

        try (var cache = mockStatic(CacheUtils.class)) {
            cache.when(() -> CacheUtils.<String>get(anyString(), any())).thenReturn(null);

            assertEquals("管理员", translation.translation(1L, null));
            assertEquals("访客,管理员", translation.translation("2,1", null));
            assertNull(translation.translation(1, null));
            assertEquals(Map.of("2,1", "访客,管理员", 1L, "管理员"),
                translation.translationBatch(new LinkedHashSet<>(List.of("2,1", 1L)), null));
        }
    }

    /**
     * 验证用户名翻译缓存未命中时回源远程服务，批量翻译按原始键顺序组装显示值。
     */
    @Test
    @DisplayName("翻译用户名")
    void shouldTranslateUserNames() {
        RemoteUserService service = mock(RemoteUserService.class);
        when(service.selectUserNameById(1L)).thenReturn("admin");
        when(service.selectListByIds(Set.of(1L, 2L))).thenReturn(List.of(user(1L, "admin"), user(2L, "guest")));
        UserNameTranslationImpl translation = new UserNameTranslationImpl(service);

        try (var cache = mockStatic(CacheUtils.class)) {
            cache.when(() -> CacheUtils.<String>get(anyString(), any())).thenReturn(null);

            assertEquals("admin", translation.translation(1L, null));
            assertEquals(Map.of("2,1", "guest,admin", 1L, "admin"),
                translation.translationBatch(new LinkedHashSet<>(List.of("2,1", 1L)), null));
        }
    }

    /**
     * 验证 OSS 翻译将键统一转为字符串后委托远程服务，批量翻译合并为一次远程查询。
     */
    @Test
    @DisplayName("翻译 OSS 访问地址")
    void shouldTranslateOssUrls() {
        RemoteFileService service = mock(RemoteFileService.class);
        when(service.selectUrlByIds("1")).thenReturn("https://file/1");
        when(service.selectUrlByIds("2,1")).thenReturn("https://file/2,https://file/1");
        when(service.selectByIds("2,1")).thenReturn(List.of(oss(1L, "https://file/1"), oss(2L, "https://file/2")));
        OssUrlTranslationImpl translation = new OssUrlTranslationImpl(service);

        assertEquals("https://file/1", translation.translation(1L, null));
        assertEquals(Map.of("2,1", "https://file/2,https://file/1", 1L, "https://file/1"),
            translation.translationBatch(new LinkedHashSet<>(List.of("2,1", 1L)), null));
    }

    /**
     * 验证字典翻译处理逗号分隔值、空片段和空字典类型，并保持原键映射关系。
     */
    @Test
    @DisplayName("批量翻译字典标签")
    void shouldTranslateDictionaryLabelsInBatch() {
        DictService service = mock(DictService.class);
        when(service.getDictLabel("status", "1")).thenReturn("启用");
        when(service.getAllDictByDictType("status")).thenReturn(Map.of("0", "停用", "1", "启用"));
        DictTypeTranslationImpl translation = new DictTypeTranslationImpl(service);

        assertEquals("启用", translation.translation("1", "status"));
        assertNull(translation.translation(1L, "status"));
        assertNull(translation.translation("1", " "));
        assertEquals(Map.of("1, ,0", "启用,停用", "0", "停用"),
            translation.translationBatch(new LinkedHashSet<>(List.of("1, ,0", "0")), "status"));
        assertEquals(Map.of(), translation.translationBatch(Set.of("1"), " "));
    }

    /**
     * 创建包含用户名的测试用户视图对象。
     *
     * @param id       用户 ID
     * @param userName 用户名
     * @return 用户视图对象
     */
    private static RemoteUserVo user(Long id, String userName) {
        RemoteUserVo user = new RemoteUserVo();
        user.setUserId(id);
        user.setUserName(userName);
        return user;
    }

    /**
     * 创建包含访问地址的测试 OSS 对象。
     *
     * @param id  OSS ID
     * @param url 访问地址
     * @return OSS 对象
     */
    private static RemoteFile oss(Long id, String url) {
        RemoteFile oss = new RemoteFile();
        oss.setOssId(id);
        oss.setUrl(url);
        return oss;
    }
}
