package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.common.core.utils.DateUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.*;
import org.dromara.system.api.domain.bo.RemoteTaskAssigneeBo;
import org.dromara.system.api.domain.vo.RemoteDeptVo;
import org.dromara.system.api.domain.vo.RemoteTaskAssigneeVo;
import org.dromara.warm.flow.core.spi.PermissionHandler;
import org.dromara.warm.flow.ui.dto.HandlerFunDto;
import org.dromara.warm.flow.ui.dto.HandlerQuery;
import org.dromara.warm.flow.ui.dto.TreeFunDto;
import org.dromara.warm.flow.ui.service.HandlerSelectService;
import org.dromara.warm.flow.ui.vo.HandlerFeedBackVo;
import org.dromara.warm.flow.ui.vo.HandlerSelectVo;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.TaskAssigneeEnum;
import org.dromara.workflow.service.IFlwSpelService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程设计器-获取办理人权限设置列表
 *
 * @author AprilWind
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwTaskAssigneeServiceImpl implements PermissionHandler, HandlerSelectService {

    private static final String DEFAULT_GROUP_NAME = "默认分组";

    @DubboReference
    private RemoteTaskAssigneeService remoteTaskAssigneeService;
    @DubboReference
    private RemoteUserService remoteUserService;
    @DubboReference
    private RemoteDeptService remoteDeptService;
    @DubboReference
    private RemoteRoleService remoteRoleService;
    @DubboReference
    private RemotePostService remotePostService;

    private final IFlwSpelService spelService;

    @Override
    public List<String> permissions() {
        return Collections.singletonList(LoginHelper.getUserIdStr());
    }

    @Override
    public String getHandler() {
        return LoginHelper.getUserIdStr();
    }

    /**
     * 将预设办理人转换为实际用户 ID 列表。
     *
     * @param permissions 预设权限标识列表
     * @return 用户 ID 列表
     */
    @Override
    public List<String> convertPermissions(List<String> permissions) {
        if (CollUtil.isEmpty(permissions)) {
            return permissions;
        }
        Map<TaskAssigneeEnum, List<String>> typeIdMap = new EnumMap<>(TaskAssigneeEnum.class);
        for (String storageId : permissions) {
            Pair<TaskAssigneeEnum, String> parsed = this.parseStorageId(storageId);
            if (parsed != null) {
                typeIdMap.computeIfAbsent(parsed.getKey(), k -> new ArrayList<>()).add(parsed.getValue());
            }
        }
        return typeIdMap.entrySet().stream()
            .map(entry -> {
                List<Long> ids = StreamUtils.toList(entry.getValue(), Convert::toLong);
                return switch (entry.getKey()) {
                    case USER -> ids;
                    case ROLE -> remoteUserService.selectUserIdsByRoleIds(ids);
                    case DEPT -> remoteUserService.selectUserIdsByDeptIds(ids);
                    case POST -> remoteUserService.selectUserIdsByPostIds(ids);
                    default -> Collections.<Long>emptyList();
                };
            })
            .filter(CollUtil::isNotEmpty)
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .distinct()
            .map(Convert::toStr)
            .toList();
    }

    /**
     * 获取办理人权限设置列表tabs页签
     *
     * @return tabs页签
     */
    @Override
    public List<String> getHandlerType() {
        return TaskAssigneeEnum.getAssigneeTypeList();
    }

    /**
     * 获取办理列表, 同时构建左侧部门树状结构
     *
     * @param query 查询条件
     * @return HandlerSelectVo
     */
    @Override
    public HandlerSelectVo getHandlerSelect(HandlerQuery query) {
        // 获取任务办理类型
        TaskAssigneeEnum type = TaskAssigneeEnum.fromDesc(query.getHandlerType());
        // 转换查询条件为 RemoteTaskAssigneeBo
        RemoteTaskAssigneeBo taskQuery = BeanUtil.toBean(query, RemoteTaskAssigneeBo.class);

        // 统一查询并构建业务数据
        RemoteTaskAssigneeVo dto = fetchTaskAssigneeData(type, taskQuery);
        List<RemoteDeptVo> depts = fetchDeptData(type);

        return getHandlerSelectVo(buildHandlerData(dto, type), buildDeptTree(depts));
    }

    /**
     * 办理人权限名称回显
     *
     * @param storageIds 入库主键集合
     * @return 结果
     */
    @Override
    public List<HandlerFeedBackVo> handlerFeedback(List<String> storageIds) {
        if (CollUtil.isEmpty(storageIds)) {
            return Collections.emptyList();
        }
        // 解析并归类 ID，同时记录原始顺序和对应解析结果
        Map<TaskAssigneeEnum, List<String>> typeIdMap = new EnumMap<>(TaskAssigneeEnum.class);
        Map<String, Pair<TaskAssigneeEnum, String>> parsedMap = new LinkedHashMap<>();
        for (String storageId : storageIds) {
            Pair<TaskAssigneeEnum, String> parsed = this.parseStorageId(storageId);
            parsedMap.put(storageId, parsed);
            if (parsed != null) {
                typeIdMap.computeIfAbsent(parsed.getKey(), k -> new ArrayList<>()).add(parsed.getValue());
            }
        }

        // 查询所有类型对应的 ID 名称映射
        Map<TaskAssigneeEnum, Map<String, String>> nameMap = this.getNamesByTypes(typeIdMap);
        // 组装返回结果，保持原始顺序
        return parsedMap.entrySet().stream()
            .map(entry -> {
                String storageId = entry.getKey();
                Pair<TaskAssigneeEnum, String> parsed = entry.getValue();
                String handlerName = (parsed == null) ? null
                    : nameMap.getOrDefault(parsed.getKey(), Collections.emptyMap())
                    .get(parsed.getValue());
                return new HandlerFeedBackVo(storageId, handlerName);
            }).toList();
    }

    /**
     * 根据任务办理类型查询对应的数据
     */
    private RemoteTaskAssigneeVo fetchTaskAssigneeData(TaskAssigneeEnum type, RemoteTaskAssigneeBo taskQuery) {
        return switch (type) {
            case USER -> remoteTaskAssigneeService.selectUsersByTaskAssigneeList(taskQuery);
            case ROLE -> remoteTaskAssigneeService.selectRolesByTaskAssigneeList(taskQuery);
            case DEPT -> remoteTaskAssigneeService.selectDeptsByTaskAssigneeList(taskQuery);
            case POST -> remoteTaskAssigneeService.selectPostsByTaskAssigneeList(taskQuery);
            case SPEL -> spelService.selectSpelByTaskAssigneeList(taskQuery);
        };
    }

    /**
     * 根据任务办理类型获取部门数据
     */
    private List<RemoteDeptVo> fetchDeptData(TaskAssigneeEnum type) {
        if (type.needsDeptService()) {
            return remoteDeptService.selectDeptsByList();
        }
        return new ArrayList<>();
    }

    /**
     * 获取权限分组名称
     *
     * @param type      任务分配人枚举
     * @param groupName 权限分组
     * @return 权限分组名称
     */
    private String getGroupName(TaskAssigneeEnum type, String groupName) {
        if (StringUtils.isEmpty(groupName)) {
            return DEFAULT_GROUP_NAME;
        }
        if (type.needsDeptService()) {
            return remoteDeptService.selectDeptNameByIds(groupName);
        }
        return DEFAULT_GROUP_NAME;
    }

    /**
     * 构建部门树状结构
     */
    private TreeFunDto<RemoteDeptVo> buildDeptTree(List<RemoteDeptVo> depts) {
        return new TreeFunDto<>(depts)
            .setId(dept -> Convert.toStr(dept.getDeptId()))
            .setName(RemoteDeptVo::getDeptName)
            .setParentId(dept -> Convert.toStr(dept.getParentId()));
    }

    /**
     * 构建任务办理人数据
     */
    private HandlerFunDto<RemoteTaskAssigneeVo.TaskHandler> buildHandlerData(RemoteTaskAssigneeVo dto, TaskAssigneeEnum type) {
        return new HandlerFunDto<>(dto.getList(), dto.getTotal())
            .setStorageId(assignee -> type.getCode() + assignee.getStorageId())
            .setHandlerCode(assignee -> StringUtils.blankToDefault(assignee.getHandlerCode(), "无"))
            .setHandlerName(assignee -> StringUtils.blankToDefault(assignee.getHandlerName(), "无"))
            .setGroupName(assignee -> this.getGroupName(type, assignee.getGroupName()))
            .setCreateTime(assignee -> DateUtils.formatLocalDateTime(assignee.getCreateTime()));
    }

    private Map<TaskAssigneeEnum, Map<String, String>> getNamesByTypes(Map<TaskAssigneeEnum, List<String>> typeIdMap) {
        Map<TaskAssigneeEnum, Map<String, String>> nameMap = new EnumMap<>(TaskAssigneeEnum.class);
        typeIdMap.forEach((type, ids) -> nameMap.put(type, this.getNamesByType(type, ids)));
        return nameMap;
    }

    /**
     * 根据任务分配类型和对应 ID 列表，批量查询名称映射关系
     *
     * @param type 分配类型（用户、角色、部门、岗位）
     * @param ids  ID 列表（如用户ID、角色ID等）
     * @return 返回 Map，其中 key 为 ID，value 为对应的名称
     */
    private Map<String, String> getNamesByType(TaskAssigneeEnum type, List<String> ids) {
        if (type == TaskAssigneeEnum.SPEL) {
            return spelService.selectRemarksBySpels(ids);
        }

        List<Long> longIds = StreamUtils.toList(ids, Convert::toLong);
        Map<Long, String> rawMap = switch (type) {
            case USER -> remoteUserService.selectUserNicksByIds(longIds);
            case ROLE -> remoteRoleService.selectRoleNamesByIds(longIds);
            case DEPT -> remoteDeptService.selectDeptNamesByIds(longIds);
            case POST -> remotePostService.selectPostNamesByIds(longIds);
            default -> Collections.emptyMap();
        };
        if (MapUtil.isEmpty(rawMap)) {
            return Collections.emptyMap();
        }
        return rawMap.entrySet()
            .stream()
            .collect(Collectors.toMap(
                e -> Convert.toStr(e.getKey()),
                Map.Entry::getValue
            ));
    }

    /**
     * 解析 storageId 字符串，返回类型和ID的组合
     *
     * @param storageId 例如 "user:123" 或 "456"
     * @return Pair(TaskAssigneeEnum, Long)，如果格式非法返回 null
     */
    private Pair<TaskAssigneeEnum, String> parseStorageId(String storageId) {
        if (StringUtils.isBlank(storageId)) {
            return null;
        }
        if (TaskAssigneeEnum.isSpelExpression(storageId)) {
            return Pair.of(TaskAssigneeEnum.SPEL, storageId);
        }
        try {
            String[] parts = storageId.split(StrUtil.COLON, 2);
            if (parts.length < 2) {
                return Pair.of(TaskAssigneeEnum.USER, parts[0]);
            } else {
                TaskAssigneeEnum type = TaskAssigneeEnum.fromCode(parts[0] + StrUtil.COLON);
                return Pair.of(type, parts[1]);
            }
        } catch (Exception e) {
            log.warn("解析 storageId 失败，格式非法：{}，错误信息：{}", storageId, e.getMessage());
            return null;
        }
    }

}
