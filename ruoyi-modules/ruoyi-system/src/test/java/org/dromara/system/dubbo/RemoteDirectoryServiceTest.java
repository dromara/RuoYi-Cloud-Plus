package org.dromara.system.dubbo;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.dromara.system.domain.SysDept;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysPostVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.mapper.SysDeptMapper;
import org.dromara.system.mapper.SysPostMapper;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.service.ISysDeptService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RemoteDirectoryServiceTest {

    @Test
    void departmentQueriesPreserveHierarchyAndSkipEmptyIdBatches() {
        var mapper = mock(SysDeptMapper.class, RETURNS_DEEP_STUBS);
        var local = mock(ISysDeptService.class);
        var remote = new RemoteDeptServiceImpl(local, mapper);
        assertNull(remote.selectById(null));
        assertTrue(remote.selectListByIds(List.of()).isEmpty());
        verifyNoInteractions(local, mapper);
        var department = new SysDeptVo();
        department.setDeptId(7L);
        department.setParentId(1L);
        department.setDeptName("Engineering");
        when(local.selectDeptById(7L)).thenReturn(department);
        when(local.selectDeptNameByIds("7")).thenReturn("Engineering");
        when(mapper.selectDeptList(any())).thenReturn(List.of(department));
        var chain = mapper.lambda();
        doReturn(chain).when(chain).select(org.mockito.ArgumentMatchers.<SFunction<SysDept, ?>>any(), any(), any());
        doReturn(chain).when(chain).select(org.mockito.ArgumentMatchers.<SFunction<SysDept, ?>>any(), any(), any(), any(), any());
        when(chain.eq(any(), eq("0"))).thenReturn(chain);
        when(chain.in(any(), anyCollection())).thenReturn(chain);
        when(chain.voList()).thenReturn(List.of(department));

        assertEquals(1L, remote.selectById(7L).getParentId());
        assertEquals("Engineering", remote.selectDeptNameByIds("7"));
        assertEquals(7L, remote.selectListByIds(List.of(7L)).getFirst().getDeptId());
        assertEquals("Engineering", remote.selectDeptsByList().getFirst().getDeptName());
    }

    @Test
    void roleQueriesPreserveRoleKeyAndDataScopeAcrossRpc() {
        var mapper = mock(SysRoleMapper.class, RETURNS_DEEP_STUBS);
        var remote = new RemoteRoleServiceImpl(mapper);
        assertNull(remote.selectById(null));
        assertTrue(remote.selectListByIds(List.of()).isEmpty());
        assertTrue(remote.selectListByUserId(null).isEmpty());
        assertTrue(remote.selectRoleNamesByIds(List.of()).isEmpty());
        verifyNoInteractions(mapper);
        var role = new SysRoleVo();
        role.setRoleId(42L);
        role.setRoleKey("reviewer");
        role.setDataScope("3");
        var row = new org.dromara.system.domain.SysRole();
        row.setRoleId(42L);
        row.setRoleName("Reviewer");
        var chain = mapper.lambda();
        doReturn(chain).when(chain).select(org.mockito.ArgumentMatchers.<SFunction<SysRole, ?>>any(), any());
        doReturn(chain).when(chain).select(org.mockito.ArgumentMatchers.<SFunction<SysRole, ?>>any(), any(), any(), any(), any());
        when(chain.in(any(), anyCollection())).thenReturn(chain);
        when(chain.list()).thenReturn(List.of(row));
        when(mapper.selectRoleById(42L)).thenReturn(role);
        when(mapper.selectRolesByUserId(7L)).thenReturn(List.of(role));
        when(mapper.selectRoleList(any())).thenReturn(List.of(role));

        assertEquals("reviewer", remote.selectById(42L).getRoleKey());
        assertEquals("3", remote.selectListByIds(List.of(42L)).getFirst().getDataScope());
        assertEquals(42L, remote.selectListByUserId(7L).getFirst().getRoleId());
        assertEquals(java.util.Map.of(42L, "Reviewer"), remote.selectRoleNamesByIds(List.of(42L)));
    }

    @Test
    void postQueriesPreserveCodeAndSkipEmptyIdBatches() {
        var mapper = mock(SysPostMapper.class, RETURNS_DEEP_STUBS);
        var remote = new RemotePostServiceImpl(mapper);
        assertNull(remote.selectById(null));
        assertTrue(remote.selectListByIds(List.of()).isEmpty());
        verifyNoInteractions(mapper);
        var entity = new org.dromara.system.domain.SysPost();
        entity.setPostId(9L);
        entity.setPostCode("qa");
        var post = new SysPostVo();
        post.setPostId(9L);
        post.setPostCode("qa");
        when(mapper.selectById(9L)).thenReturn(entity);
        var chain = mapper.lambda();
        when(chain.in(any(), anyCollection())).thenReturn(chain);
        doReturn(chain).when(chain).orderByAsc(any(), any());
        when(mapper.selectPostList(any())).thenReturn(List.of(post));

        assertEquals("qa", remote.selectById(9L).getPostCode());
        assertEquals(9L, remote.selectListByIds(List.of(9L)).getFirst().getPostId());
    }
}
