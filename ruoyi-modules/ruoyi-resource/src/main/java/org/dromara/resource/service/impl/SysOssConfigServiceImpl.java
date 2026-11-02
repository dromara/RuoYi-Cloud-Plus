package org.dromara.resource.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.ObjectUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.common.oss.constant.OssConstant;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.resource.domain.SysOssConfig;
import org.dromara.resource.domain.bo.SysOssConfigBo;
import org.dromara.resource.domain.vo.SysOssConfigVo;
import org.dromara.resource.event.OssConfigChangeEvent;
import org.dromara.resource.mapper.SysOssConfigMapper;
import org.dromara.resource.service.ISysOssConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 对象存储配置Service业务层处理
 *
 * @author Lion Li
 * @author 孤舟烟雨
 * @date 2021-08-13
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysOssConfigServiceImpl implements ISysOssConfigService {

    private final SysOssConfigMapper ossConfigMapper;

    /**
     * 项目启动时，初始化参数到缓存，加载配置类
     */
    @Override
    public void init() {
        try {
            List<SysOssConfig> list = ossConfigMapper.selectList();
            if (CollUtil.isEmpty(list)) {
                log.warn("OSS配置初始化未查询到配置数据");
                return;
            }

            boolean hasDefaultConfig = false;
            // 加载OSS初始化配置
            for (SysOssConfig config : list) {
                String configKey = config.getConfigKey();
                if (SystemConstants.YES.equals(config.getStatus())) {
                    RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, configKey);
                    hasDefaultConfig = true;
                }
                CacheUtils.put(CacheNames.SYS_OSS_CONFIG, configKey, JsonUtils.toJsonString(config));
            }
            if (!hasDefaultConfig) {
                log.warn("OSS配置初始化未找到启用的默认配置");
            }
        } catch (RuntimeException e) {
            log.error("OSS配置初始化失败", e);
            throw e;
        }
    }

    @Override
    public SysOssConfigVo queryById(Long ossConfigId) {
        return ossConfigMapper.selectVoById(ossConfigId);
    }

    @Override
    public PageResult<SysOssConfigVo> queryPageList(SysOssConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysOssConfig> lqw = buildQueryWrapper(bo);
        Page<SysOssConfigVo> result = ossConfigMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }


    private LambdaQueryWrapper<SysOssConfig> buildQueryWrapper(SysOssConfigBo bo) {
        return QueryBuilder.lambda(SysOssConfig.class)
            .eqIfText(SysOssConfig::getConfigKey, bo.getConfigKey())
            .likeIfText(SysOssConfig::getBucketName, bo.getBucketName())
            .eqIfText(SysOssConfig::getStatus, bo.getStatus())
            .orderByAsc(SysOssConfig::getOssConfigId)
            .build();
    }

    @Override
    public Boolean insertByBo(SysOssConfigBo bo) {
        SysOssConfig config = MapstructUtils.convert(bo, SysOssConfig.class);
        validEntityBeforeSave(config);
        boolean flag = ossConfigMapper.insert(config) > 0;
        if (flag) {
            // 从数据库查询完整的数据做缓存
            config = ossConfigMapper.selectById(config.getOssConfigId());
            publishOssConfigSaved(config, null);
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(SysOssConfigBo bo) {
        SysOssConfig config = MapstructUtils.convert(bo, SysOssConfig.class);
        validEntityBeforeSave(config);
        SysOssConfig oldConfig = ossConfigMapper.selectById(config.getOssConfigId());
        boolean flag = ossConfigMapper.lambda()
            .set(ObjectUtil.isNull(config.getPrefix()), SysOssConfig::getPrefix, "")
            .set(ObjectUtil.isNull(config.getRegion()), SysOssConfig::getRegion, "")
            .set(ObjectUtil.isNull(config.getExt1()), SysOssConfig::getExt1, "")
            .set(ObjectUtil.isNull(config.getRemark()), SysOssConfig::getRemark, "")
            .eq(SysOssConfig::getOssConfigId, config.getOssConfigId())
            .update(config);
        if (flag) {
            // 从数据库查询完整的数据做缓存
            config = ossConfigMapper.selectById(config.getOssConfigId());
            publishOssConfigSaved(config, ObjectUtils.notNullGetter(oldConfig, SysOssConfig::getConfigKey));
        }
        return flag;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysOssConfig entity) {
        if (StringUtils.isNotEmpty(entity.getConfigKey()) && !checkConfigKeyUnique(entity)) {
            throw new ServiceException("操作配置'{}'失败, 配置key已存在!", entity.getConfigKey());
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            if (CollUtil.containsAny(ids, OssConstant.SYSTEM_DATA_IDS)) {
                throw new ServiceException("系统内置, 不可删除!");
            }
        }
        List<SysOssConfig> list = CollUtil.newArrayList();
        for (Long configId : ids) {
            SysOssConfig config = ossConfigMapper.selectById(configId);
            if (ObjectUtil.isNotNull(config)) {
                list.add(config);
            }
        }
        boolean flag = ossConfigMapper.deleteByIds(ids) > 0;
        if (flag) {
            list.forEach(sysOssConfig ->
                SpringUtils.context().publishEvent(OssConfigChangeEvent.remove(sysOssConfig.getConfigKey())));
        }
        return flag;
    }

    /**
     * 判断configKey是否唯一
     */
    private boolean checkConfigKeyUnique(SysOssConfig sysOssConfig) {
        long ossConfigId = ObjectUtils.notNull(sysOssConfig.getOssConfigId(), -1L);
        SysOssConfig info = ossConfigMapper.lambda()
            .select(SysOssConfig::getOssConfigId, SysOssConfig::getConfigKey)
            .eq(SysOssConfig::getConfigKey, sysOssConfig.getConfigKey())
            .one();
        return ObjectUtil.isNull(info) || ObjectUtil.equals(info.getOssConfigId(), ossConfigId);
    }

    /**
     * 启用禁用状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateOssConfigStatus(SysOssConfigBo bo) {
        SysOssConfig sysOssConfig = MapstructUtils.convert(bo, SysOssConfig.class);
        int row = ossConfigMapper.lambda().set(SysOssConfig::getStatus, SystemConstants.NO).updateCount();
        row += ossConfigMapper.updateById(sysOssConfig);
        if (row > 0) {
            SpringUtils.context().publishEvent(OssConfigChangeEvent.useDefault(sysOssConfig.getConfigKey()));
        }
        return row;
    }

    /**
     * 发布 OSS 配置保存事件。
     *
     * @param config       当前配置
     * @param oldConfigKey 变更前配置 key
     */
    private void publishOssConfigSaved(SysOssConfig config, String oldConfigKey) {
        SpringUtils.context().publishEvent(OssConfigChangeEvent.save(
            config.getConfigKey(),
            oldConfigKey,
            JsonUtils.toJsonString(config)
        ));
    }

}
