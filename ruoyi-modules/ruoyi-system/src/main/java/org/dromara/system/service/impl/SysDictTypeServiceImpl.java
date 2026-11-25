package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.system.domain.SysDictData;
import org.dromara.system.domain.SysDictType;
import org.dromara.system.domain.bo.SysDictTypeBo;
import org.dromara.system.domain.vo.SysDictDataVo;
import org.dromara.system.domain.vo.SysDictTypeVo;
import org.dromara.system.mapper.SysDictDataMapper;
import org.dromara.system.mapper.SysDictTypeMapper;
import org.dromara.system.service.ISysDictTypeService;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 字典 业务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysDictTypeServiceImpl implements ISysDictTypeService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;

    /**
     * 分页查询字典类型列表
     *
     * @param dictType  查询条件
     * @param pageQuery 分页参数
     * @return 字典类型分页列表
     */
    @Override
    public PageResult<SysDictTypeVo> selectPageDictTypeList(SysDictTypeBo dictType, PageQuery pageQuery) {
        LambdaQueryWrapper<SysDictType> lqw = buildQueryWrapper(dictType);
        Page<SysDictTypeVo> page = dictTypeMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 根据条件查询字典类型
     *
     * @param dictType 字典类型信息
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictTypeVo> selectDictTypeList(SysDictTypeBo dictType) {
        LambdaQueryWrapper<SysDictType> lqw = buildQueryWrapper(dictType);
        return dictTypeMapper.selectVoList(lqw);
    }

    /**
     * 构造字典类型列表查询条件。
     *
     * @param bo 字典类型筛选条件
     * @return 包含字典名称、字典类型筛选条件和字典ID升序排序的查询包装器
     */
    private LambdaQueryWrapper<SysDictType> buildQueryWrapper(SysDictTypeBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(SysDictType.class)
            .likeIfText(SysDictType::getDictName, bo.getDictName())
            .likeIfText(SysDictType::getDictType, bo.getDictType())
            .betweenParams(SysDictType::getCreateTime, params, "beginTime", "endTime")
            .orderByAsc(SysDictType::getDictId)
            .build();
    }

    /**
     * 根据所有字典类型
     *
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictTypeVo> selectDictTypeAll() {
        return dictTypeMapper.selectVoList();
    }

    /**
     * 根据字典类型查询字典数据
     *
     * @param dictType 字典类型
     * @return 字典数据集合信息
     */
    @Cacheable(cacheNames = CacheNames.SYS_DICT, key = "#dictType")
    @Override
    public List<SysDictDataVo> selectDictDataByType(String dictType) {
        List<SysDictDataVo> dictDatas = dictDataMapper.selectDictDataByType(dictType);
        return CollUtil.isNotEmpty(dictDatas) ? dictDatas : Collections.emptyList();
    }

    /**
     * 根据字典类型ID查询信息
     *
     * @param dictId 字典类型ID
     * @return 字典类型
     */
    @Override
    public SysDictTypeVo selectDictTypeById(Long dictId) {
        return dictTypeMapper.selectVoById(dictId);
    }

    /**
     * 根据字典类型查询信息
     *
     * @param dictType 字典类型
     * @return 字典类型
     */
    @Cacheable(cacheNames = CacheNames.SYS_DICT_TYPE, key = "#dictType")
    @Override
    public SysDictTypeVo selectDictTypeByType(String dictType) {
        return dictTypeMapper.lambda().eq(SysDictType::getDictType, dictType).voOne();
    }

    /**
     * 批量删除字典类型信息
     *
     * @param dictIds 需要删除的字典ID
     */
    @Override
    public void deleteDictTypeByIds(List<Long> dictIds) {
        List<SysDictType> list = dictTypeMapper.selectByIds(dictIds);
        list.forEach(x -> {
            boolean assigned = dictDataMapper.lambda().eq(SysDictData::getDictType, x.getDictType()).exists();
            if (assigned) {
                throw new ServiceException("{}已分配,不能删除", x.getDictName());
            }
        });
        dictTypeMapper.deleteByIds(dictIds);
        list.forEach(x -> {
            CacheUtils.evict(CacheNames.SYS_DICT, x.getDictType());
            CacheUtils.evict(CacheNames.SYS_DICT_TYPE, x.getDictType());
        });
    }

    /**
     * 重置字典缓存数据
     */
    @Override
    public void resetDictCache() {
        CacheUtils.clear(CacheNames.SYS_DICT);
        CacheUtils.clear(CacheNames.SYS_DICT_TYPE);
    }

    /**
     * 新增保存字典类型信息
     *
     * @param bo 字典类型信息
     * @return 结果
     */
    @CachePut(cacheNames = CacheNames.SYS_DICT, key = "#bo.dictType")
    @Override
    public List<SysDictDataVo> insertDictType(SysDictTypeBo bo) {
        SysDictType dict = MapstructUtils.convert(bo, SysDictType.class);
        int row = dictTypeMapper.insert(dict);
        if (row > 0) {
            CacheUtils.evict(CacheNames.SYS_DICT_TYPE, dict.getDictType());
            // 新增 type 下无 data 数据 返回空防止缓存穿透
            return new ArrayList<>();
        }
        throw new ServiceException("操作失败");
    }

    /**
     * 修改保存字典类型信息
     *
     * @param bo 字典类型信息
     * @return 结果
     */
    @CachePut(cacheNames = CacheNames.SYS_DICT, key = "#bo.dictType")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SysDictDataVo> updateDictType(SysDictTypeBo bo) {
        SysDictType dict = MapstructUtils.convert(bo, SysDictType.class);
        SysDictType oldDict = dictTypeMapper.selectById(dict.getDictId());
        if (oldDict == null) {
            throw new ServiceException("字典类型不存在，无法修改");
        }
        int row = dictTypeMapper.updateById(dict);
        if (row > 0) {
            if (!StringUtils.equals(oldDict.getDictType(), dict.getDictType())) {
                dictDataMapper.lambda()
                    .set(SysDictData::getDictType, dict.getDictType())
                    .eq(SysDictData::getDictType, oldDict.getDictType())
                    .update();
                CacheUtils.evict(CacheNames.SYS_DICT_TYPE, dict.getDictType());
            }
            CacheUtils.evict(CacheNames.SYS_DICT, oldDict.getDictType());
            return dictDataMapper.selectDictDataByType(dict.getDictType());
        }
        throw new ServiceException("操作失败");
    }

    /**
     * 校验字典类型称是否唯一
     *
     * @param dictType 字典类型
     * @return 结果
     */
    @Override
    public boolean checkDictTypeUnique(SysDictTypeBo dictType) {
        boolean exist = dictTypeMapper.lambda()
            .eq(SysDictType::getDictType, dictType.getDictType())
            .neIfPresent(SysDictType::getDictId, dictType.getDictId())
            .exists();
        return !exist;
    }

}
