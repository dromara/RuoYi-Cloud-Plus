package org.dromara.system.domain.convert;

import org.dromara.system.api.domain.vo.RemoteTenantVo;
import org.dromara.system.domain.vo.SysTenantVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 租户转换器
 * @author zhujie
 */
@Mapper
public interface SysTenantVoConvert {

    SysTenantVoConvert INSTANCE = Mappers.getMapper(SysTenantVoConvert.class);

    /**
     * SysTenantVoToRemoteTenantVo
     * @param sysTenantVo 待转换对象
     * @return 转换后对象
     */
    RemoteTenantVo convert(SysTenantVo sysTenantVo);

    /**
     * SysTenantVoListToRemoteTenantVoList
     * @param sysTenantVos 待转换对象
     * @return 转换后对象
     */
    List<RemoteTenantVo> convertList(List<SysTenantVo> sysTenantVos);
}
