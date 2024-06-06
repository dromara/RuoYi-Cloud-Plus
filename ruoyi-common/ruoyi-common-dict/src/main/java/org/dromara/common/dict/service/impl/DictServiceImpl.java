package org.dromara.common.dict.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.common.core.constant.CacheConstants;
import org.dromara.common.core.service.DictService;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.system.api.RemoteDictService;
import org.dromara.system.api.domain.vo.RemoteDictDataVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 字典服务服务
 *
 * @author Lion Li
 */
@Service
public class DictServiceImpl implements DictService {

    @Autowired
    private Cache<Object, Object> ceffeine;

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
    @SuppressWarnings("unchecked")
    @Override
    public String getDictLabel(String dictType, String dictValue, String separator) {
        // 优先从本地缓存获取
        List<RemoteDictDataVo> datas = (List<RemoteDictDataVo>) ceffeine.get(CacheConstants.SYS_DICT_KEY + "remote:" + dictType, k -> {
            return remoteDictService.selectDictDataByType(dictType);
        });
        Map<String, String> map = StreamUtils.toMap(datas, RemoteDictDataVo::getDictValue, RemoteDictDataVo::getDictLabel);
        if (StringUtils.containsAny(dictValue, separator)) {
            return Arrays.stream(dictValue.split(separator))
                .map(v -> map.getOrDefault(v, StringUtils.EMPTY))
                .collect(Collectors.joining(separator));
        } else {
            return map.getOrDefault(dictValue, StringUtils.EMPTY);
        }
    }

    /**
     * 根据字典类型和字典标签获取字典值
     *
     * @param dictType  字典类型
     * @param dictLabel 字典标签
     * @param separator 分隔符
     * @return 字典值
     */
    @SuppressWarnings("unchecked")
    @Override
    public String getDictValue(String dictType, String dictLabel, String separator) {
        // 优先从本地缓存获取
        List<RemoteDictDataVo> datas = (List<RemoteDictDataVo>) ceffeine.get(CacheConstants.SYS_DICT_KEY + "remote:" + dictType, k -> {
            return remoteDictService.selectDictDataByType(dictType);
        });
        Map<String, String> map = StreamUtils.toMap(datas, RemoteDictDataVo::getDictLabel, RemoteDictDataVo::getDictValue);
        if (StringUtils.containsAny(dictLabel, separator)) {
            return Arrays.stream(dictLabel.split(separator))
                .map(l -> map.getOrDefault(l, StringUtils.EMPTY))
                .collect(Collectors.joining(separator));
        } else {
            return map.getOrDefault(dictLabel, StringUtils.EMPTY);
        }
    }

    @Override
    public Map<String, String> getAllDictByDictType(String dictType) {
        List<RemoteDictDataVo> list = remoteDictService.selectDictDataByType(dictType);
        return StreamUtils.toMap(list, RemoteDictDataVo::getDictValue, RemoteDictDataVo::getDictLabel);
    }

}
