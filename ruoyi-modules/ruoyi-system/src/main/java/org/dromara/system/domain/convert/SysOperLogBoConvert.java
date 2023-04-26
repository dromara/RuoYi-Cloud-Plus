package org.dromara.system.domain.convert;

import org.dromara.system.api.domain.bo.RemoteOperLogBo;
import org.dromara.system.domain.bo.SysOperLogBo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 操作日志转换器
 * @author zhujie
 */
@Mapper
public interface SysOperLogBoConvert {

    SysOperLogBoConvert INSTANCE = Mappers.getMapper(SysOperLogBoConvert.class);

    /**
     * RemoteOperLogBoToSysOperLogBo
     * @param remoteOperLogBo 待转换对象
     * @return 转换后对象
     */
    @Mapping(target = "businessTypes", ignore = true)
    SysOperLogBo convert(RemoteOperLogBo remoteOperLogBo);
}
