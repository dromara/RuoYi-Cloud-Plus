package org.dromara.system.domain.convert;

import org.dromara.system.api.domain.vo.RemoteDictDataVo;
import org.dromara.system.domain.vo.SysDictDataVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 字典数据转换器
 * @author zhujie
 */
@Mapper
public interface SysDictDataVoConvert {

    SysDictDataVoConvert INSTANCE = Mappers.getMapper(SysDictDataVoConvert.class);

    /**
     * SysDictDataVoToRemoteDictDataVo
     * @param sysDictDataVo 待转换对象
     * @return 转换后对象
     */
    RemoteDictDataVo convert(SysDictDataVo sysDictDataVo);

    /**
     * SysDictDataVoListToRemoteDictDataVoList
     * @param sysDictDataVos 待转换对象
     * @return 转换后对象
     */
    List<RemoteDictDataVo> convertList(List<SysDictDataVo> sysDictDataVos);
}
