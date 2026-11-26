package org.dromara.system.dubbo;

import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.exception.user.UserException;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysPostVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RemoteUserServiceTest {

    private final SysUserMapper mapper = mock(SysUserMapper.class, RETURNS_DEEP_STUBS);
    private final ISysUserService users = mock(ISysUserService.class);
    private final ISysPermissionService permissions = mock(ISysPermissionService.class);
    private final ISysRoleService roles = mock(ISysRoleService.class);
    private final ISysDeptService departments = mock(ISysDeptService.class);
    private final ISysPostService posts = mock(ISysPostService.class);
    private final ISysConfigService configuration = mock(ISysConfigService.class);
    private final RemoteUserServiceImpl service =
        new RemoteUserServiceImpl(users, permissions, configuration, roles, departments, posts, mapper);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void phoneAndEmailLookupsRequireAnActiveAccount(boolean phone) {
        String contact = phone ? "15500000001" : "alice@example.org";
        var query = mapper.lambda();
        when(query.eq(any(), eq(contact))).thenReturn(query);
        var active = user("alice", SystemConstants.NORMAL);
        active.setUserId(42L);
        when(query.voOne()).thenReturn(null, user("alice", SystemConstants.DISABLE), active);
        if (phone) {
            assertEquals("user.not.exists", assertThrows(UserException.class, () -> service.getUserInfoByPhoneNumber(contact)).getCode());
            assertEquals("user.blocked", assertThrows(UserException.class, () -> service.getUserInfoByPhoneNumber(contact)).getCode());
            assertEquals("alice", service.getUserInfoByPhoneNumber(contact).getUsername());
        } else {
            assertEquals("user.not.exists", assertThrows(UserException.class, () -> service.getUserInfoByEmail(contact)).getCode());
            assertEquals("user.blocked", assertThrows(UserException.class, () -> service.getUserInfoByEmail(contact)).getCode());
            assertEquals("alice", service.getUserInfoByEmail(contact).getUsername());
        }
    }

    @Test
    void openIdIsCarriedInMiniProgramResult() {
        assertEquals("open-42", service.getUserInfoByOpenid("open-42").getOpenid());
    }

    @Test
    void remoteRegistrationEnforcesConfigurationAndDuplicateCheck() {
        var input = new org.dromara.system.api.domain.bo.RemoteUserBo();
        input.setUserName("alice");
        var local = new org.dromara.system.domain.bo.SysUserBo();
        local.setUserName("alice");
        var query = mapper.lambda();
        when(query.eq(any(), eq("alice"))).thenReturn(query);
        when(query.exists()).thenReturn(true, false);
        when(configuration.selectConfigByKey("sys.account.registerUser")).thenReturn("false", "true", "true");
        when(users.registerUser(local)).thenReturn(true);
        try (var beans = mockStatic(cn.hutool.extra.spring.SpringUtil.class)) {
            var converter = mock(io.github.linpeilie.Converter.class);
            beans.when(() -> cn.hutool.extra.spring.SpringUtil.getBean(io.github.linpeilie.Converter.class)).thenReturn(converter);
            org.dromara.common.core.utils.MapstructUtils.convert((org.dromara.system.api.domain.bo.RemoteUserBo) null, org.dromara.system.domain.bo.SysUserBo.class);
            try (var mapping = mockStatic(org.dromara.common.core.utils.MapstructUtils.class)) {
                mapping.when(() -> org.dromara.common.core.utils.MapstructUtils.convert(input,
                    org.dromara.system.domain.bo.SysUserBo.class)).thenReturn(local);
                assertThrows(org.dromara.common.core.exception.ServiceException.class, () -> service.registerUserInfo(input));
                assertThrows(UserException.class, () -> service.registerUserInfo(input));
                assertTrue(service.registerUserInfo(input));
                verify(users).registerUser(local);
            }
        }
    }

    @Test
    void recordsLoginIpAndAuditableOperator() {
        service.recordLoginInfo(42L, "192.0.2.10");
        var updated = org.mockito.ArgumentCaptor.forClass(org.dromara.system.domain.SysUser.class);
        verify(mapper).updateById(updated.capture());
        assertEquals(42L, updated.getValue().getUserId());
        assertEquals(42L, updated.getValue().getUpdateBy());
        assertEquals("192.0.2.10", updated.getValue().getLoginIp());
        assertNotNull(updated.getValue().getLoginDate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"different", "missing", "disabled"})
    void rejectsUnavailableAccountsBeforeLoadingPermissions(String scenario) {
        SysUserVo user = switch (scenario) {
            case "different" -> user("Alice", SystemConstants.NORMAL);
            case "disabled" -> user("alice", SystemConstants.DISABLE);
            default -> null;
        };
        var query = mapper.lambda();
        when(query.eq(any(), eq("alice"))).thenReturn(query);
        when(query.voOne()).thenReturn(user);

        assertThrows(UserException.class, () -> service.getUserInfo("alice"));
        verifyNoInteractions(permissions, roles, departments, posts);
    }

    @Test
    void returnsCanonicalAccountAndAuthorizationContext() {
        var user = user("alice", SystemConstants.NORMAL);
        user.setUserId(42L);
        user.setDeptId(7L);
        var department = new SysDeptVo();
        department.setDeptName("Engineering");
        department.setDeptCategory("team");
        var role = new SysRoleVo();
        role.setRoleId(9L);
        role.setRoleKey("reviewer");
        var post = new SysPostVo();
        post.setPostId(12L);
        when(mapper.selectVoById(42L)).thenReturn(user);
        when(departments.selectDeptById(7L)).thenReturn(department);
        when(permissions.getMenuPermission(42L)).thenReturn(Set.of("system:user:list"));
        when(permissions.getRolePermission(42L)).thenReturn(Set.of("reviewer"));
        when(permissions.getDataScopeRoleMap(anyList())).thenReturn(Map.of("system:user:list", List.of(9L)));
        when(roles.selectRolesByUserId(42L)).thenReturn(List.of(role));
        when(posts.selectPostsByUserId(42L)).thenReturn(List.of(post));

        var login = service.getUserInfo(42L);

        assertEquals(42L, login.getUserId());
        assertEquals("alice", login.getUsername());
        assertEquals("Engineering", login.getDeptName());
        assertEquals("team", login.getDeptCategory());
        assertEquals(Set.of("system:user:list"), login.getMenuPermission());
        assertEquals(Set.of("reviewer"), login.getRolePermission());
        assertEquals(9L, login.getRoles().getFirst().getRoleId());
        assertEquals(12L, login.getPosts().getFirst().getPostId());
        assertEquals(Map.of("system:user:list", List.of(9L)), login.getDataScopeRoleMap());
    }

    @Test
    void emptyBatchLookupsDoNotReachStorage() {
        assertTrue(service.selectListByIds(List.of()).isEmpty());
        assertTrue(service.selectUsersByRoleIds(List.of()).isEmpty());
        assertTrue(service.selectUserNamesByIds(List.of()).isEmpty());
        assertTrue(service.selectUserNicksByIds(List.of()).isEmpty());
        assertTrue(service.selectUsersByDeptIds(List.of()).isEmpty());
        verifyNoInteractions(mapper, users);
    }

    @Test
    void remoteDirectoryReturnsAccountContactFieldsAndMembershipIds() {
        var local = user("alice", SystemConstants.NORMAL);
        local.setUserId(42L);
        local.setPhoneNumber("15500000001");
        when(users.selectUserByUserName("alice")).thenReturn(local);
        when(users.selectUserByPhoneNumber("15500000001")).thenReturn(local);
        when(users.selectUserNameById(42L)).thenReturn("alice");
        when(users.selectNicknameById(42L)).thenReturn("Alice");
        when(users.selectNicknameByIds("42")).thenReturn("Alice");
        when(users.selectPhonenumberById(42L)).thenReturn("15500000001");
        when(users.selectEmailById(42L)).thenReturn("alice@example.org");
        when(users.selectUserIdsByDeptIds(List.of(7L))).thenReturn(List.of(42L));
        when(users.selectUserIdsByPostIds(List.of(9L))).thenReturn(List.of(42L));
        when(users.selectUserIdsByPostIds(List.of(8L))).thenReturn(List.of());

        assertNull(service.selectByUserName(" "));
        assertNull(service.selectByPhoneNumber(" "));
        assertEquals(42L, service.selectByUserName("alice").getUserId());
        assertEquals("15500000001", service.selectByPhoneNumber("15500000001").getPhoneNumber());
        assertEquals("alice", service.selectUserNameById(42L));
        assertEquals("Alice", service.selectNicknameById(42L));
        assertEquals("Alice", service.selectNicknameByIds("42"));
        assertEquals("15500000001", service.selectPhonenumberById(42L));
        assertEquals("alice@example.org", service.selectEmailById(42L));
        assertEquals(List.of(42L), service.selectUserIdsByDeptIds(List.of(7L)));
        assertEquals(List.of(42L), service.selectUserIdsByPostIds(List.of(9L)));
        assertTrue(service.selectUsersByPostIds(List.of(8L)).isEmpty());
    }

    @Test
    void departmentMembershipReturnsUserDataFromTheFilteredQuery() {
        var row = new org.dromara.system.domain.SysUser();
        row.setUserId(42L);
        row.setUserName("alice");
        var query = mapper.lambda();
        doReturn(query).when(query).select(
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.toolkit.support.SFunction<org.dromara.system.domain.SysUser, ?>>any(),
            any(), any(), any(), any());
        when(query.eq(any(), eq(SystemConstants.NORMAL))).thenReturn(query);
        when(query.in(any(), eq(List.of(7L)))).thenReturn(query);
        when(query.list()).thenReturn(List.of(row));

        var result = service.selectUsersByDeptIds(List.of(7L));

        assertEquals(42L, result.getFirst().getUserId());
        assertEquals("alice", result.getFirst().getUserName());
        verify(query).in(any(), eq(List.of(7L)));
    }

    private static SysUserVo user(String username, String status) {
        var user = new SysUserVo();
        user.setUserName(username);
        user.setStatus(status);
        return user;
    }
}
