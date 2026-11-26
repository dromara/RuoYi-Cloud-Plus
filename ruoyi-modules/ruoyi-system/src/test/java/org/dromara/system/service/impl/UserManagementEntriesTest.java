package org.dromara.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysUserExportVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysPostVo;
import org.dromara.system.mapper.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserManagementEntriesTest {
    private final SysUserMapper users = mock(SysUserMapper.class);
    private final SysDeptMapper departments = mock(SysDeptMapper.class);
    private final SysUserRoleMapper assignments = mock(SysUserRoleMapper.class);
    private final SysRoleMapper roles = mock(SysRoleMapper.class);
    private final SysPostMapper posts = mock(SysPostMapper.class);
    private final SysUserServiceImpl service = new SysUserServiceImpl(users, departments, roles, posts, assignments, mock());

    @Test
    void userListsRetainTotalsAndExportUsesDepartmentHierarchy() {
        var criteria = new SysUserBo();
        criteria.setDeptId(7L);
        criteria.setRoleId(9L);
        criteria.setExcludeUserIds("8,10");
        var query = new PageQuery(10, 2);
        var page = new Page<SysUserVo>(2, 10, 24);
        var detail = new SysUserExportVo();
        when(departments.selectDeptAndChildById(7L)).thenReturn(List.of(7L, 11L));
        when(users.selectPageUserList(any(), any())).thenReturn(page);
        when(users.selectAllocatedList(any(), same(criteria))).thenReturn(page);
        when(assignments.selectUserIdsByRoleId(9L)).thenReturn(List.of(42L));
        when(users.selectUnallocatedList(any(), same(criteria), eq(List.of(42L)))).thenReturn(page);
        when(users.selectUserExportList(criteria, List.of(7L, 11L))).thenReturn(List.of(detail));

        assertEquals(24L, service.selectPageUserList(criteria, query).getTotal());
        assertEquals(24L, service.selectAllocatedList(criteria, query).getTotal());
        assertEquals(24L, service.selectUnallocatedList(criteria, query).getTotal());
        assertEquals(List.of(detail), service.selectUserExportList(criteria));
    }

    @Test
    void uniquenessChecksExcludeTheAccountBeingEdited() {
        initializeUserMapping();
        var criteria = new SysUserBo();
        criteria.setUserId(42L);
        criteria.setUserName("alice");
        criteria.setPhoneNumber("15500000001");
        criteria.setEmail("alice@example.org");
        when(users.currentModelClass()).thenReturn(SysUser.class);
        when(users.lambda()).thenAnswer(call -> new LambdaCrudChainWrapper<>(users));
        doAnswer(call -> {
            LambdaCrudChainWrapper<SysUser, SysUserVo> query = call.getArgument(0);
            assertTrue(query.getSqlSegment().contains("user_id <>"));
            assertTrue(query.getParamNameValuePairs().containsValue(42L));
            return query.getSqlSegment().contains("email");
        }).when(users).exists(any());

        assertTrue(service.checkUserNameUnique(criteria));
        assertTrue(service.checkPhoneUnique(criteria));
        assertFalse(service.checkEmailUnique(criteria));
    }

    @Test
    void statusProfileAndPasswordWritesReturnTheirAffectedRowCount() {
        initializeUserMapping();
        when(users.currentModelClass()).thenReturn(SysUser.class);
        when(users.lambda()).thenAnswer(call -> new LambdaCrudChainWrapper<>(users));
        doAnswer(call -> {
            LambdaCrudChainWrapper<SysUser, SysUserVo> query = call.getArgument(0);
            assertTrue(query.getSqlSegment().contains("user_id ="));
            assertTrue(query.getParamNameValuePairs().containsValue(42L));
            return 1;
        }).when(users).update(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        var profile = new SysUserBo();
        profile.setUserId(42L);
        profile.setNickName("Alice");

        assertEquals(1, service.updateUserStatus(42L, "1"));
        assertEquals(1, service.updateUserProfile(profile));
        assertEquals(1, service.resetUserPwd(42L, "new-password-hash"));
    }

    @Test
    void accountLookupsReturnContactFieldsAndOrganizationMemberships() {
        var entity = new SysUser();
        entity.setUserName("alice");
        entity.setNickName("Alice");
        entity.setPhoneNumber("15500000001");
        entity.setEmail("alice@example.org");
        var record = new SysUserVo();
        record.setUserId(42L);
        var role = new SysRoleVo();
        role.setRoleName("Reviewer");
        var post = new SysPostVo();
        post.setPostName("Engineering");
        when(users.selectVoById(42L)).thenReturn(record);
        when(roles.selectRolesByUserId(42L)).thenReturn(List.of(role));
        when(posts.selectPostsByUserId(42L)).thenReturn(List.of(post));
        when(users.selectUserList(any())).thenReturn(List.of(record));
        when(users.currentModelClass()).thenReturn(SysUser.class);
        when(users.lambda()).thenAnswer(call -> new LambdaCrudChainWrapper<>(users));
        when(users.selectOne(any())).thenReturn(entity);

        assertEquals(List.of(role), service.selectUserById(42L).getRoles());
        assertNull(service.selectUserById(99L));
        assertEquals(List.of(record), service.selectUserByIds(List.of(42L), 7L));
        assertEquals(List.of(record), service.selectUserListByDept(7L));
        assertEquals("Reviewer", service.selectUserRoleGroup(42L));
        assertEquals("Engineering", service.selectUserPostGroup(42L));
        assertEquals("alice", service.selectUserNameById(42L));
        assertEquals("Alice", service.selectNicknameById(42L));
        assertEquals("15500000001", service.selectPhonenumberById(42L));
        assertEquals("alice@example.org", service.selectEmailById(42L));
    }

    private static void initializeUserMapping() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "users"), SysUser.class);
    }
}
