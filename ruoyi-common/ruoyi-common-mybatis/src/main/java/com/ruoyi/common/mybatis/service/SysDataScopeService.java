package com.ruoyi.common.mybatis.service;

import com.ruoyi.system.api.RemoteDataScopeService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 数据权限服务
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
