package org.dromara.system.api;

import org.dromara.system.api.domain.vo.RemoteDeptVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 部门服务
 *
 * @author Lion Li
 */
public interface RemoteDeptService {

    /**
     * 通过部门ID查询部门名称
     *
     * @param deptIds 部门ID串逗号分隔
     * @return 部门名称串逗号分隔
     */
    String selectDeptNameByIds(String deptIds);

    /**
     * 根据部门ID查询部门负责人
     *
     * @param deptId 部门ID，用于指定需要查询的部门
     * @return 返回该部门的负责人ID
     */
    Long selectDeptLeaderById(Long deptId);

    /**
     * 查询部门
     *
     * @return 部门列表
     */
    List<RemoteDeptVo> selectDeptsByList();

    /**
     * 根据部门 ID 列表查询部门名称映射关系
     *
     * @param deptIds 部门 ID 列表
     * @return Map，其中 key 为部门 ID，value 为对应的部门名称
     */
    Map<Long, String> selectDeptNamesByIds(Collection<Long> deptIds);

    /**
     * 根据 ID 查询部门。
     *
     * @param deptId 部门 ID
     * @return 部门信息
     */
    RemoteDeptVo selectById(Long deptId);

    /**
     * 根据 ID 集合查询数据权限范围内的部门。
     *
     * @param deptIds 部门 ID 集合
     * @return 部门列表
     */
    List<RemoteDeptVo> selectListByIds(Collection<Long> deptIds);

    /**
     * 查询正常状态的子部门 ID，不包含自身。
     *
     * @param deptId 部门 ID
     * @return 数据权限范围内的子部门 ID
     */
    List<Long> selectChildDeptIds(Long deptId);

    /**
     * 查询本部门及正常状态的子部门 ID。
     *
     * @param deptId 部门 ID
     * @return 数据权限范围内的部门 ID
     */
    List<Long> selectDeptAndChildIds(Long deptId);

}
