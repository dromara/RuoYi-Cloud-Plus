package org.dromara.auth.domain.convert;

import org.dromara.auth.domain.vo.TenantListVo;
import org.dromara.system.api.domain.vo.RemoteTenantVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 租户vo转换器
 * @author zhujie
 */
@Mapper
public interface TenantVoConvert {

    TenantVoConvert INSTANCE = Mappers.getMapper(TenantVoConvert.class);

    /**
     * RemoteTenantVoToTenantListVo
     * @param remoteTenantVo 待转换对象
     * @return 转换后对象
     */
    TenantListVo convert(RemoteTenantVo remoteTenantVo);

    /**
     * RemoteTenantVoToTenantListVo
     * @param remoteTenantVo 待转换对象
     * @return 转换后对象
     */
    List<TenantListVo> convertList(List<RemoteTenantVo> remoteTenantVo);



}
