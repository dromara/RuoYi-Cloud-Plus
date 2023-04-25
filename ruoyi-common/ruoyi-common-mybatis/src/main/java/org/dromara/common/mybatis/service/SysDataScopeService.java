package org.dromara.common.mybatis.service;

import org.dromara.system.api.RemoteDataScopeService;
import org.apache.dubbo.config.annotation.DubboReference;
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

    public String getRoleCustom(Long roleId) {
        return remoteDataScopeService.getRoleCustom(roleId);
    }

    public String getDeptAndChild(Long deptId) {
        return remoteDataScopeService.getDeptAndChild(deptId);
    }
}
