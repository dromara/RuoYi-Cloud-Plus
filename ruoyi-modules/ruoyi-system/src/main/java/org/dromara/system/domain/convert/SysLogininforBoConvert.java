package org.dromara.system.domain.convert;

import org.dromara.system.api.domain.bo.RemoteLogininforBo;
import org.dromara.system.domain.bo.SysLogininforBo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 登录日志转换器
 * @author zhujie
 */
@Mapper
public interface SysLogininforBoConvert {

    SysLogininforBoConvert INSTANCE = Mappers.getMapper(SysLogininforBoConvert.class);

    /**
     * RemoteLogininforBoToSysLogininforBo
     * @param remoteLogininforBo 待转换对象
     * @return 转换后对象
     */
    SysLogininforBo convert(RemoteLogininforBo remoteLogininforBo);
}
