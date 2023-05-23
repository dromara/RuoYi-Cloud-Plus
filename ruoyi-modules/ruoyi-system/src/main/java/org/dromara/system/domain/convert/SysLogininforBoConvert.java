package org.dromara.system.domain.convert;

import io.github.linpeilie.BaseMapper;
import org.dromara.system.api.domain.bo.RemoteLogininforBo;
import org.dromara.system.domain.bo.SysLogininforBo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 登录日志转换器
 * @author zhujie
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SysLogininforBoConvert extends BaseMapper<RemoteLogininforBo, SysLogininforBo> {

}
