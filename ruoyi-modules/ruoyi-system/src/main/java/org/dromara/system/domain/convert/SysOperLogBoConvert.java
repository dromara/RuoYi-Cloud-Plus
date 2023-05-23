package org.dromara.system.domain.convert;

import io.github.linpeilie.BaseMapper;
import org.dromara.system.api.domain.bo.RemoteOperLogBo;
import org.dromara.system.domain.bo.SysOperLogBo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * 操作日志转换器
 * @author zhujie
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SysOperLogBoConvert extends BaseMapper<RemoteOperLogBo, SysOperLogBo> {

    /**
     * RemoteOperLogBoToSysOperLogBo
     * @param remoteOperLogBo 待转换对象
     * @return 转换后对象
     */
    @Mapping(target = "businessTypes", ignore = true)
    SysOperLogBo convert(RemoteOperLogBo remoteOperLogBo);
}
