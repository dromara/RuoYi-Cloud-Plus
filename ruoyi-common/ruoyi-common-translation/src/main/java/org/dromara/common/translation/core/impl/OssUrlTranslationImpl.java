package org.dromara.common.translation.core.impl;

import lombok.AllArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.common.translation.core.TranslationInterface;
import org.dromara.resource.api.RemoteFileService;
import org.dromara.resource.api.domain.RemoteFile;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OSS翻译实现
 *
 * @author Lion Li
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.OSS_ID_TO_URL)
public class OssUrlTranslationImpl implements TranslationInterface<String> {

    @DubboReference(mock = "true")
    private RemoteFileService remoteFileService;

    /**
     * 将 OSS ID 或 ID 集合翻译为访问地址。
     *
     * @param key   OSS ID 或逗号分隔的 ID 字符串
     * @param other 额外参数
     * @return 访问地址
     */
    @Override
    public String translation(Object key, String other) {
        return remoteFileService.selectUrlByIds(key.toString());
    }

    /**
     * 批量将 OSS ID 翻译为访问地址。
     *
     * @param keys  OSS ID 集合
     * @param other 额外参数
     * @return OSS ID 与访问地址映射
     */
    @Override
    public Map<Object, String> translationBatch(Set<Object> keys, String other) {
        return translateLongOrStringKeys(keys, ids -> {
            String idText = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            return StreamUtils.toMap(remoteFileService.selectByIds(idText), RemoteFile::getOssId, RemoteFile::getUrl);
        });
    }

}
