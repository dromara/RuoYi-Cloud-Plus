package org.dromara.system.api;

import org.dromara.system.api.model.PostDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 岗位服务
 *
 * @author Lion Li
 */
public interface RemotePostService {

    /**
     * 根据岗位 ID 列表查询岗位名称映射关系
     *
     * @param postIds 岗位 ID 列表
     * @return Map，其中 key 为岗位 ID，value 为对应的岗位名称
     */
    Map<Long, String> selectPostNamesByIds(Collection<Long> postIds);

    /**
     * 根据 ID 查询岗位，包含停用岗位。
     *
     * @param postId 岗位 ID
     * @return 岗位信息
     */
    PostDTO selectById(Long postId);

    /**
     * 根据 ID 集合查询岗位。
     *
     * @param postIds 岗位 ID 集合
     * @return 数据权限范围内的岗位列表
     */
    List<PostDTO> selectListByIds(Collection<Long> postIds);

    /**
     * 查询用户关联的岗位。
     *
     * @param userId 用户 ID
     * @return 岗位列表
     */
    List<PostDTO> selectListByUserId(Long userId);

    /**
     * 查询部门下的岗位。
     *
     * @param deptId 部门 ID
     * @return 数据权限范围内的岗位列表
     */
    List<PostDTO> selectListByDeptId(Long deptId);

}
