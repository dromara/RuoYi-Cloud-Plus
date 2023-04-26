package org.dromara.system.domain.convert;

import org.dromara.system.api.domain.bo.RemoteUserBo;
import org.dromara.system.domain.bo.SysUserBo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 用户信息转换器
 * @author zhujie
 */
@Mapper
public interface SysUserBoConvert {

    SysUserBoConvert INSTANCE = Mappers.getMapper(SysUserBoConvert.class);

    /**
     * RemoteUserBoToSysUserBo
     * @param remoteUserBo 待转换对象
     * @return 转换后对象
     */
    @Mapping(target = "roleIds", ignore = true)
    @Mapping(target = "postIds", ignore = true)
    SysUserBo convert(RemoteUserBo remoteUserBo);
}
