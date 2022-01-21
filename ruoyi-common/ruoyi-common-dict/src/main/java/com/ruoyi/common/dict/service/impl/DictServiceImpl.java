package com.ruoyi.common.dict.service.impl;

import com.ruoyi.common.core.service.DictService;
import com.ruoyi.system.api.RemoteDictService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 字典服务服务
 *
 * @author Lion Li
 */
@Service
public class DictServiceImpl implements DictService {

    @DubboReference
    private RemoteDictService remoteDictService;

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     * @param separator 分隔符
     * @return 字典标签
     */
    @Override
    public String getDictLabel(String dictType, String dictValue, String separator) {
        return remoteDictService.getDictLabel(dictType, dictValue, separator);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     *
     * @param dictType  字典类型
     * @param dictLabel 字典标签
     * @param separator 分隔符
     * @return 字典值
     */
    @Override
    public String getDictValue(String dictType, String dictLabel, String separator) {
        return remoteDictService.getDictValue(dictType, dictLabel, separator);
    }

}
