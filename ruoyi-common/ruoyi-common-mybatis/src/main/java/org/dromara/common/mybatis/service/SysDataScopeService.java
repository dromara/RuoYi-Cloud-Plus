package org.dromara.common.mybatis.service;

import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.system.api.RemoteDataScopeService;
import org.springframework.stereotype.Service;

/**
 * 数据权限 实现
 * <p>
 * 注意: 此Service内不允许调用标注`数据权限`注解的方法
 * 例如: deptMapper.selectList 此 selectList 方法标注了`数据权限`注解 会出现循环解析的问题
 *
 * @author Lion Li
 */
@Service("sdss")
public class SysDataScopeService {

    @DubboReference
    private RemoteDataScopeService remoteDataScopeService;

    /**
     * 获取角色自定义权限语句
     *
     * @param roleId 角色ID
     * @return 返回角色的自定义权限语句，如果没有找到则返回 null
     */
    public String getRoleCustom(Long roleId) {
        return remoteDataScopeService.getRoleCustom(roleId);
    }

    /**
     * 获取部门和下级权限语句
     *
     * @param deptId 部门ID
     * @return 返回部门及其下级的权限语句，如果没有找到则返回 null
     */
    public String getDeptAndChild(Long deptId) {
        return remoteDataScopeService.getDeptAndChild(deptId);
    }

}
