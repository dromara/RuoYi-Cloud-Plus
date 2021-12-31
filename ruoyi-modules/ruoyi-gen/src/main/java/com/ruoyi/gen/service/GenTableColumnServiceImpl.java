package com.ruoyi.gen.service;

import cn.hutool.core.convert.Convert;
import com.ruoyi.gen.domain.GenTableColumn;
import com.ruoyi.gen.mapper.GenTableColumnMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务字段 服务层实现
 *
 * @author ruoyi
 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class GenTableColumnServiceImpl implements IGenTableColumnService {

    private final GenTableColumnMapper genTableColumnMapper;

    /**
     * 查询业务字段列表
     *
     * @param tableId 业务字段编号
     * @return 业务字段集合
     */
    @Override
    public List<GenTableColumn> selectGenTableColumnListByTableId(Long tableId) {
        return genTableColumnMapper.selectGenTableColumnListByTableId(tableId);
    }

    /**
     * 新增业务字段
     *
     * @param genTableColumn 业务字段信息
     * @return 结果
     */
    @Override
    public int insertGenTableColumn(GenTableColumn genTableColumn) {
        return genTableColumnMapper.insertGenTableColumn(genTableColumn);
    }

    /**
     * 修改业务字段
     *
     * @param genTableColumn 业务字段信息
     * @return 结果
     */
    @Override
    public int updateGenTableColumn(GenTableColumn genTableColumn) {
        return genTableColumnMapper.updateGenTableColumn(genTableColumn);
    }

    /**
     * 删除业务字段对象
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteGenTableColumnByIds(String ids) {
        return genTableColumnMapper.deleteGenTableColumnByIds(Convert.toLongArray(ids));
    }
}