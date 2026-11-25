package org.dromara.system.controller.system;

import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.R;
import org.dromara.system.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemDeletionControllerTest {

    @Test
    void menuWithChildrenCannotBeDeleted() {
        var menus = mock(ISysMenuService.class);
        when(menus.hasChildByMenuId(15L)).thenReturn(true);

        var response = new SysMenuController(menus).remove(15L);

        assertEquals("存在子菜单,不允许删除", response.getMsg());
        verify(menus, never()).deleteMenuById(15L);
    }

    @Test
    void rootDepartmentCannotBeDeleted() {
        var departments = mock(ISysDeptService.class);
        var response = new SysDeptController(departments, mock()).remove(Long.valueOf(SystemConstants.ROOT_DEPT_ANCESTORS));

        assertEquals("根部门不允许删除", response.getMsg());
        verifyNoInteractions(departments);
    }

}
