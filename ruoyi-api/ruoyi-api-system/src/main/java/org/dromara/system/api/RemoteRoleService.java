package org.dromara.system.api;

import org.dromara.system.api.model.RoleDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 角色服务
 *
 * @author Lion Li
 */
public interface RemoteRoleService {

    /**
     * 根据角色 ID 列表查询角色名称映射关系
     *
     * @param roleIds 角色 ID 列表
     * @return Map，其中 key 为角色 ID，value 为对应的角色名称
     */
    Map<Long, String> selectRoleNamesByIds(Collection<Long> roleIds);

    /**
     * 根据 ID 查询角色。
     *
     * @param roleId 角色 ID
     * @return 角色信息
     */
    RoleDTO selectById(Long roleId);

    /**
     * 根据 ID 集合查询角色。
     *
     * @param roleIds 角色 ID 集合
     * @return 数据权限范围内的角色列表
     */
    List<RoleDTO> selectListByIds(Collection<Long> roleIds);

    /**
     * 查询用户关联的角色。
     *
     * @param userId 用户 ID
     * @return 角色列表
     */
    List<RoleDTO> selectListByUserId(Long userId);

}
