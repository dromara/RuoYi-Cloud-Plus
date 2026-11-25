package org.dromara.resource.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.enums.PushSourceEnum;
import org.dromara.common.core.enums.PushTypeEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.dromara.common.push.dto.PushPayloadDTO;
import org.dromara.resource.domain.SysMessage;
import org.dromara.resource.domain.vo.SysMessageBoxVo;
import org.dromara.resource.domain.vo.SysMessageVo;
import org.dromara.resource.mapper.SysMessageMapper;
import org.dromara.resource.service.ISysMessageService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息记录服务实现
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysMessageServiceImpl implements ISysMessageService {

    /**
     * 全局广播用户标识（所有用户可见）
     */
    private static final String GLOBAL_USER_IDS = "0";

    /**
     * 消息分类：系统消息
     */
    private static final String CATEGORY_SYSTEM = "system";

    /**
     * 消息分类：通知公告
     */
    private static final String CATEGORY_NOTICE = "notice";

    /**
     * 消息分类：工作流
     */
    private static final String CATEGORY_WORKFLOW = "workflow";

    /**
     * 消息盒子每页展示最大条数
     */
    private static final int BOX_LIMIT = 100;

    /**
     * 消息盒子展示消息天数（仅展示30天内）
     */
    private static final long BOX_DAYS = 30L;

    private final SysMessageMapper messageMapper;

    /**
     * 查询当前用户消息盒子数据
     * 按系统消息、通知公告、工作流消息分类返回
     *
     * @param userId 用户ID
     * @return 分类消息盒子数据
     */
    @Override
    public SysMessageBoxVo queryMessageBox(Long userId) {
        SysMessageBoxVo box = new SysMessageBoxVo();
        box.setSystemList(selectMessageList(CATEGORY_SYSTEM, userId));
        box.setNoticeList(selectMessageList(CATEGORY_NOTICE, userId));
        box.setWorkflowList(selectMessageList(CATEGORY_WORKFLOW, userId));
        return box;
    }

    /**
     * 存储全局广播消息到数据库
     *
     * @param payload 消息推送体
     * @return 回填消息ID后的消息体
     */
    @Override
    public PushPayloadDTO storeAll(PushPayloadDTO payload) {
        return storeMessage(null, payload);
    }

    /**
     * 存储指定用户消息到数据库
     *
     * @param userIds 用户ID集合
     * @param payload 消息推送体
     * @return 回填消息ID后的消息体
     */
    @Override
    public PushPayloadDTO storeUsers(List<Long> userIds, PushPayloadDTO payload) {
        return CollUtil.isEmpty(userIds) ? payload : storeMessage(userIds, payload);
    }

    /**
     * 统一消息存储逻辑
     * 判断是否需要存入消息盒子，需要则插入数据库
     *
     * @param userIds 用户ID集合（为null则全局广播）
     * @param payload 消息推送体
     * @return 回填消息ID后的消息体
     */
    private PushPayloadDTO storeMessage(List<Long> userIds, PushPayloadDTO payload) {
        if (!supportsMessageBox(payload)) {
            return payload;
        }
        SysMessage message = buildMessage(userIds, payload);
        if (messageMapper.insert(message) == 0) {
            throw new ServiceException("消息保存失败");
        }
        payload.setMessageId(message.getMessageId());
        return payload;
    }

    /**
     * 根据分类和用户ID查询消息列表
     * 仅查询30天内、最多100条、按时间倒序
     *
     * @param category 消息分类
     * @param userId   用户ID
     * @return 消息VO列表
     */
    private List<SysMessageVo> selectMessageList(String category, Long userId) {
        List<SysMessage> list = messageMapper.lambda()
            .eq(SysMessage::getCategory, category)
            .ge(SysMessage::getCreateTime, LocalDateTime.now().minusDays(BOX_DAYS))
            .and(wrapper ->
                wrapper.eq(SysMessage::getSendUserIds, GLOBAL_USER_IDS)
                    .or()
                    .findInSet(userId, SysMessage::getSendUserIds)
            )
            .orderByDesc(SysMessage::getCreateTime, SysMessage::getMessageId)
            .list(new Page<>(1, BOX_LIMIT, false));
        return list.stream().map(this::buildVo).toList();
    }

    /**
     * 构建消息实体（用于数据库存储）
     *
     * @param userIds 接收用户ID集合
     * @param payload 消息推送体
     * @return 系统消息实体
     */
    private SysMessage buildMessage(List<Long> userIds, PushPayloadDTO payload) {
        SysMessage message = new SysMessage();
        message.setMessageId(payload.getMessageId() == null ? IdGeneratorUtil.nextLongId() : payload.getMessageId());
        message.setCategory(resolveCategory(payload));
        message.setType(payload.getType());
        message.setSource(payload.getSource());
        message.setTitle(resolveTitle(payload));
        message.setMessage(payload.getMessage());
        message.setContent(resolveContent(payload));
        message.setDataJson(JsonUtils.toJsonString(payload.getData()));
        message.setPath(payload.getPath());
        message.setSendUserIds(CollUtil.isEmpty(userIds) ? GLOBAL_USER_IDS : StringUtils.joinComma(userIds));
        return message;
    }

    /**
     * 消息实体转换为展示VO
     *
     * @param entity 消息实体
     * @return 消息展示VO
     */
    private SysMessageVo buildVo(SysMessage entity) {
        SysMessageVo vo = new SysMessageVo();
        vo.setMessageId(entity.getMessageId());
        vo.setCategory(entity.getCategory());
        vo.setType(entity.getType());
        vo.setSource(entity.getSource());
        vo.setTitle(entity.getTitle());
        vo.setMessage(entity.getMessage());
        vo.setContent(entity.getContent());
        vo.setPath(entity.getPath());
        vo.setCreateTime(entity.getCreateTime());
        vo.setData(parseData(entity.getDataJson()));
        return vo;
    }

    /**
     * 判断消息是否需要存入消息盒子
     * 仅系统消息、通知消息需要存入
     *
     * @param payload 消息推送体
     * @return 是否支持存入消息盒子
     */
    private boolean supportsMessageBox(PushPayloadDTO payload) {
        if (payload == null) {
            return false;
        }
        if (StringUtils.equalsAny(payload.getType(), PushTypeEnum.MESSAGE.getType(), PushTypeEnum.NOTICE.getType())) {
            return !StringUtils.equalsAny(payload.getType(), PushTypeEnum.LLM.getType())
                && !StringUtils.equalsAny(payload.getSource(), PushSourceEnum.LLM.getSource());
        }
        return false;
    }

    /**
     * 根据消息类型/来源自动解析消息分类
     *
     * @param payload 消息推送体
     * @return 消息分类（system/notice/workflow）
     */
    private String resolveCategory(PushPayloadDTO payload) {
        if (StringUtils.equalsAny(payload.getType(), PushTypeEnum.NOTICE.getType())
            || StringUtils.equalsAny(payload.getSource(), PushSourceEnum.NOTICE.getSource())) {
            return CATEGORY_NOTICE;
        }
        if (StringUtils.equalsAny(payload.getSource(), PushSourceEnum.WORKFLOW.getSource())) {
            return CATEGORY_WORKFLOW;
        }
        return CATEGORY_SYSTEM;
    }

    /**
     * 根据消息分类自动生成消息标题
     *
     * @param payload 消息推送体
     * @return 消息标题
     */
    private String resolveTitle(PushPayloadDTO payload) {
        return switch (resolveCategory(payload)) {
            case CATEGORY_NOTICE -> "通知公告消息";
            case CATEGORY_WORKFLOW -> "工作流消息";
            default -> "系统消息";
        };
    }

    /**
     * 解析消息内容（从data中提取noticeContent）
     *
     * @param payload 消息推送体
     * @return 消息内容
     */
    private String resolveContent(PushPayloadDTO payload) {
        Object data = payload.getData();
        if (data instanceof Map<?, ?> map) {
            return Convert.toStr(map.get("noticeContent"));
        }
        return null;
    }

    /**
     * 解析JSON数据字符串为对象
     *
     * @param dataJson JSON字符串
     * @return 解析后对象
     */
    private Object parseData(String dataJson) {
        if (StringUtils.isBlank(dataJson)) {
            return null;
        }
        return JsonUtils.parseObject(dataJson, Object.class);
    }
}
