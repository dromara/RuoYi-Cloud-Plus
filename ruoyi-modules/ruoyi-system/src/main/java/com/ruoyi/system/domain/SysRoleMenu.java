package com.ruoyi.system.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 角色和菜单关联 sys_role_menu
 *
 * @author ruoyi
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class SysRoleMenu {

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 菜单ID
     */
    private Long menuId;

}
