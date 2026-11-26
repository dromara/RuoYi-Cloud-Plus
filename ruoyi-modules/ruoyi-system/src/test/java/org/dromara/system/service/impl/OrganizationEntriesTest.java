package org.dromara.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.enums.DataBaseType;
import org.dromara.common.mybatis.helper.DataBaseHelper;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysDept;
import org.dromara.system.domain.SysPost;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysDeptBo;
import org.dromara.system.domain.bo.SysPostBo;
import org.dromara.system.domain.bo.SysRoleBo;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysPostVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.mapper.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganizationEntriesTest {
    @Test
    void departmentQueriesPreservePageTotalAndHandleMissingRecords() {
        var mapper = mock(SysDeptMapper.class);
        var service = new SysDeptServiceImpl(mapper, mock(), mock());
        var page = new Page<SysDeptVo>(2, 10, 13);
        when(mapper.selectPageDeptList(any(), any())).thenReturn(page);
        when(mapper.deleteById(7L)).thenReturn(1);

        assertEquals(13L, service.selectPageDeptList(new SysDeptBo(), new PageQuery(10, 2)).getTotal());
        assertNull(service.selectDeptById(7L));
        assertEquals(1, service.deleteDeptById(7L));
        verify(mapper).deleteById(7L);
    }

    @Test
    void roleQueriesPreservePageTotalAndAssignedRoleCannotBeDisabled() {
        var mapper = mock(SysRoleMapper.class);
        var service = spy(new SysRoleServiceImpl(mapper, mock(), mock(), mock()));
        var role = new SysRoleVo();
        var page = new Page<SysRoleVo>(1, 10, 17);
        page.setRecords(List.of(role));
        when(mapper.selectPageRoleList(any(), any())).thenReturn(page);
        when(mapper.selectRoleById(7L)).thenReturn(role);
        doReturn(1L).when(service).countUserRoleByRoleId(7L);

        assertEquals(17L, service.selectPageRoleList(new SysRoleBo(), new PageQuery()).getTotal());
        assertSame(role, service.selectRoleById(7L));
        assertThrows(ServiceException.class, () -> service.updateRoleStatus(7L, SystemConstants.DISABLE));
        verify(mapper, never()).lambda();
    }

    @Test
    void postQueriesPreserveRecordsAndSingleDeletionUsesTheSameValidationAsBatch() {
        var mapper = mock(SysPostMapper.class);
        var service = new SysPostServiceImpl(mapper, mock(), mock());
        var post = new SysPostVo();
        var page = new Page<SysPostVo>(1, 10, 17);
        page.setRecords(List.of(post));
        when(mapper.selectPagePostList(any(), any())).thenReturn(page);
        when(mapper.selectVoById(7L)).thenReturn(post);
        when(mapper.selectByIds(List.of(7L))).thenReturn(List.of());
        when(mapper.deleteByIds(List.of(7L))).thenReturn(1);

        assertEquals(17L, service.selectPagePostList(new SysPostBo(), new PageQuery()).getTotal());
        assertSame(post, service.selectPostById(7L));
        assertEquals(1, service.deletePostById(7L));
        var ordered = inOrder(mapper);
        ordered.verify(mapper).selectByIds(List.of(7L));
        ordered.verify(mapper).deleteByIds(List.of(7L));
    }
    @Test
    void departmentDirectoryChecksHierarchyUsageAndAccess() {
        var mapper = mock(SysDeptMapper.class);
        var users = mock(SysUserMapper.class);
        var service = new SysDeptServiceImpl(mapper, mock(), users);
        var dept = new SysDeptVo();
        dept.setDeptId(7L);
        dept.setParentId(0L);
        dept.setDeptName("Engineering");
        dept.setOrderNum(1);
        when(mapper.currentModelClass()).thenReturn(SysDept.class);
        when(mapper.lambda()).thenAnswer(call -> new LambdaCrudChainWrapper<>(mapper));
        when(users.currentModelClass()).thenReturn(SysUser.class);
        when(users.lambda()).thenAnswer(call -> new LambdaCrudChainWrapper<>(users));
        when(mapper.selectDeptList(any())).thenReturn(List.of(dept));
        when(mapper.selectCount(any())).thenReturn(2L);
        when(mapper.exists(any())).thenReturn(true, false);
        when(users.exists(any())).thenReturn(true);
        var criteria = new SysDeptBo();
        criteria.setDeptName("Engineering");
        criteria.setParentId(0L);

        assertEquals(List.of(dept), service.selectDeptList(criteria));
        assertEquals(7L, service.selectDeptTreeList(criteria).getFirst().getId());
        assertEquals(List.of(dept), service.selectDeptByIds(List.of(7L)));
        try (var database = mockStatic(DataBaseHelper.class)) {
            database.when(DataBaseHelper::getDataBaseType).thenReturn(DataBaseType.MY_SQL);
            database.when(() -> DataBaseHelper.findInSet(any(), anyString())).thenCallRealMethod();
            assertEquals(2L, service.selectNormalChildrenDeptById(7L));
        }
        assertTrue(service.hasChildByDeptId(7L));
        assertTrue(service.checkDeptExistUser(7L));
        assertTrue(service.checkDeptNameUnique(criteria));
        try (var login = mockStatic(LoginHelper.class)) {
            assertThrows(ServiceException.class, () -> service.checkDeptDataScope(7L));
        }
    }

    @Test
    void roleDirectoryMarksAssignmentsAndChecksUniquenessAndDataScope() {
        var mapper = mock(SysRoleMapper.class);
        var menus = mock(SysRoleMenuMapper.class);
        var departments = mock(SysRoleDeptMapper.class);
        var assignments = mock(SysUserRoleMapper.class);
        var service = new SysRoleServiceImpl(mapper, menus, assignments, departments);
        var assigned = new SysRoleVo();
        assigned.setRoleId(7L);
        var available = new SysRoleVo();
        available.setRoleId(8L);
        when(mapper.selectRolesByUserId(42L)).thenReturn(List.of(assigned));
        when(mapper.selectRoleList(any())).thenReturn(List.of(assigned, available));
        when(mapper.currentModelClass()).thenReturn(SysRole.class);
        when(mapper.lambda()).thenAnswer(call -> new LambdaCrudChainWrapper<>(mapper));
        when(mapper.exists(any())).thenReturn(true, false);
        when(mapper.selectRoleCount(List.of(7L))).thenReturn(1L);
        when(assignments.lambda()).thenAnswer(call -> new LambdaCrudChainWrapper<>(assignments));
        when(assignments.selectCount(any())).thenReturn(3L);
        when(menus.lambda()).thenAnswer(call -> new LambdaCrudChainWrapper<>(menus));
        when(departments.lambda()).thenAnswer(call -> new LambdaCrudChainWrapper<>(departments));
        when(mapper.deleteById(7L)).thenReturn(1);
        var criteria = new SysRoleBo();
        criteria.setRoleName("Reviewer");
        criteria.setRoleKey("reviewer");

        assertTrue(service.selectRolesAuthByUserId(42L).getFirst().isFlag());
        assertFalse(available.isFlag());
        assertEquals(List.of(assigned), service.selectRolesByUserId(42L));
        assertEquals(List.of(7L), service.selectRoleListByUserId(42L));
        assertEquals(2, service.selectRoleByIds(List.of(7L, 8L)).size());
        assertFalse(service.checkRoleNameUnique(criteria));
        assertTrue(service.checkRoleKeyUnique(criteria));
        assertEquals(3L, service.countUserRoleByRoleId(7L));
        try (var login = mockStatic(LoginHelper.class)) {
            assertDoesNotThrow(() -> service.checkRoleDataScope(7L));
            assertThrows(ServiceException.class, () -> service.checkRoleDataScope(8L));
        }
        assertEquals(1, service.deleteRoleById(7L));
        var writes = inOrder(menus, departments, mapper);
        writes.verify(menus).delete(any());
        writes.verify(departments).delete(any());
        writes.verify(mapper).deleteById(7L);
    }

    @Test
    void postDirectoryReadsActiveRowsAndCountsDepartmentAndUserAssignments() {
        var mapper = mock(SysPostMapper.class);
        var assignments = mock(SysUserPostMapper.class);
        var service = new SysPostServiceImpl(mapper, mock(), assignments);
        var post = new SysPostVo();
        post.setPostId(7L);
        when(mapper.currentModelClass()).thenReturn(SysPost.class);
        when(mapper.lambda()).thenAnswer(call -> new LambdaCrudChainWrapper<>(mapper));
        when(mapper.selectVoList(any())).thenReturn(List.of(post));
        when(mapper.selectPostsByUserId(42L)).thenReturn(List.of(post));
        when(mapper.selectCount(any())).thenReturn(2L);
        when(assignments.lambda()).thenAnswer(call -> new LambdaCrudChainWrapper<>(assignments));
        when(assignments.selectCount(any())).thenReturn(1L);

        assertEquals(List.of(post), service.selectPostList(new SysPostBo()));
        assertEquals(List.of(post), service.selectPostsByUserId(42L));
        assertEquals(List.of(post), service.selectPostAll());
        assertEquals(List.of(post), service.selectPostByIds(List.of(7L)));
        assertEquals(1L, service.countUserPostById(7L));
        assertEquals(2L, service.countPostByDeptId(8L));
    }
}
