package org.dromara.common.log.domain.convert;

import org.dromara.common.log.event.OperLogEvent;
import org.dromara.system.api.domain.bo.RemoteOperLogBo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 操作日志转换器
 * @author zhujie
 */
@Mapper
public interface OperLogEventConvert {

    OperLogEventConvert INSTANCE = Mappers.getMapper(OperLogEventConvert.class);

    /**
     * OperLogEventToRemoteOperLogBo
     * @param operLogEvent 待转换对象
     * @return 转换后对象
     */
    @Mapping(target = "params", ignore = true)
    RemoteOperLogBo convert(OperLogEvent operLogEvent);
}
