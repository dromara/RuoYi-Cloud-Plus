package org.dromara.system.dubbo;

import cn.hutool.core.lang.Dict;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.system.api.RemoteConfigService;
import org.dromara.system.service.ISysConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 配置服务
 *
 * @author Michelle.Chung
 */
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteConfigServiceImpl implements RemoteConfigService {

    private final ISysConfigService configService;

    /**
     * 获取注册开关
     */
    @Override
    public boolean selectRegisterEnabled() {
        return configService.selectRegisterEnabled();
    }

    @Override
    public String getConfigValue(String configKey) {
        return configService.selectConfigByKey(configKey);
    }

    /**
     * 根据参数 key 获取 Map 类型的配置
     *
     * @param configKey 参数 key
     * @return Dict 对象；配置为 null、空字符串或纯空白时返回 null
     * @throws RuntimeException 配置不是有效 JSON 或无法转换为目标类型
     */
    @Override
    public Dict getConfigMap(String configKey) {
        String configValue = getConfigValue(configKey);
        return JsonUtils.parseMap(configValue);
    }

    /**
     * 根据参数 key 获取 Map 类型的配置列表
     *
     * @param configKey 参数 key
     * @return Dict 列表；配置为 null、空字符串或纯空白时返回 null
     * @throws RuntimeException 配置不是有效 JSON 或无法转换为目标类型
     */
    @Override
    public List<Dict> getConfigArrayMap(String configKey) {
        String configValue = getConfigValue(configKey);
        return JsonUtils.parseArrayMap(configValue);
    }

    /**
     * 根据参数 key 获取指定类型的配置对象
     *
     * @param configKey 参数 key
     * @param clazz     目标对象类型
     * @param <T>       目标对象泛型
     * @return 对象实例；配置为 null 或空字符串时返回 null
     * @throws RuntimeException 配置不是有效 JSON 或无法转换为目标类型
     */
    @Override
    public <T> T getConfigObject(String configKey, Class<T> clazz) {
        String configValue = getConfigValue(configKey);
        return JsonUtils.parseObject(configValue, clazz);
    }

    /**
     * 根据参数 key 获取指定类型的配置列表
     *
     * @param configKey 参数 key
     * @param clazz     目标元素类型
     * @param <T>       元素类型泛型
     * @return 指定类型列表；配置为 null 或空字符串时返回空列表，JSON null 返回 null
     * @throws RuntimeException 配置不是有效 JSON 或无法转换为目标类型
     */
    @Override
    public <T> List<T> getConfigArray(String configKey, Class<T> clazz) {
        String configValue = getConfigValue(configKey);
        return JsonUtils.parseArray(configValue, clazz);
    }


}
