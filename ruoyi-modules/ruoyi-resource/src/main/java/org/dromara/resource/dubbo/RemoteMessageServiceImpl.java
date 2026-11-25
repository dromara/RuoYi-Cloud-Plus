package org.dromara.resource.dubbo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.common.core.enums.PushSourceEnum;
import org.dromara.common.core.enums.PushTypeEnum;
import org.dromara.common.push.dto.PushPayloadDTO;
import org.dromara.common.push.helper.PushHelper;
import org.dromara.resource.api.RemoteMessageService;
import org.dromara.resource.api.domain.RemotePushPayLoad;
import org.dromara.resource.service.ISysMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息服务
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteMessageServiceImpl implements RemoteMessageService {

    private final ISysMessageService sysMessageService;

    /**
     * 发送消息
     *
     * @param userIds 用户ID列表
     * @param message 消息文本
     */
    @Override
    public void publishMessage(List<Long> userIds, String message) {
        publishMessagePayload(userIds, RemotePushPayLoad.of(PushTypeEnum.MESSAGE, PushSourceEnum.BACKEND, message, null));
    }

    @Override
    public void publishMessagePayload(List<Long> userIds, RemotePushPayLoad payload) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        PushPayloadDTO pushPayload = BeanUtil.copyProperties(payload, PushPayloadDTO.class);
        PushHelper.publishMessage(userIds, sysMessageService.storeUsers(userIds, pushPayload));
    }

    /**
     * 发布订阅的消息(群发)
     *
     * @param message 消息内容
     */
    @Override
    public void publishAll(String message) {
        publishAllPayload(RemotePushPayLoad.of(PushTypeEnum.MESSAGE, PushSourceEnum.BACKEND, message, null));
    }

    @Override
    public void publishAllPayload(RemotePushPayLoad payload) {
        PushPayloadDTO pushPayload = BeanUtil.copyProperties(payload, PushPayloadDTO.class);
        PushHelper.publishAll(sysMessageService.storeAll(pushPayload));
    }

}
