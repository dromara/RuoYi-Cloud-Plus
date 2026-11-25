package org.dromara.common.translation.core.impl;

import cn.hutool.core.convert.Convert;
import lombok.AllArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.common.translation.core.TranslationInterface;
import org.dromara.system.api.RemoteUserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户昵称翻译实现
 *
 * @author may
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.USER_ID_TO_NICKNAME)
public class NicknameTranslationImpl implements TranslationInterface<String> {

    @DubboReference
    private RemoteUserService remoteUserService;

    /**
     * 将用户 ID 或 ID 集合翻译为用户昵称。
     *
     * @param key   用户 ID 或逗号分隔的 ID 字符串
     * @param other 额外参数
     * @return 用户昵称
     */
    @Override
    public String translation(Object key, String other) {
        if (key instanceof Long id) {
            String nickname = CacheUtils.get(CacheNames.SYS_NICKNAME, key);
            if (StringUtils.isNotBlank(nickname)) {
                return nickname;
            }
            return remoteUserService.selectNicknameById(id);
        } else if (key instanceof String ids) {
            List<String> list = new ArrayList<>();
            for (Long id : StringUtils.splitTo(ids, Convert::toLong)) {
                String nickname = CacheUtils.get(CacheNames.SYS_NICKNAME, id);
                if (StringUtils.isNotBlank(nickname)) {
                    list.add(nickname);
                } else {
                    list.add(remoteUserService.selectNicknameById(id));
                }
            }
            return StringUtils.joinComma(list);
        }
        return null;
    }

    /**
     * 批量将用户 ID 翻译为用户昵称。
     *
     * @param keys  用户 ID 集合
     * @param other 额外参数
     * @return 用户 ID 与用户昵称映射
     */
    @Override
    public Map<Object, String> translationBatch(Set<Object> keys, String other) {
        return translateLongOrStringKeys(keys, remoteUserService::selectUserNicksByIds);
    }

}
