package com.ruoyi.system.dubbo;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.system.api.RemoteDataScopeService;
import com.ruoyi.system.api.domain.SysDept;
import com.ruoyi.system.domain.SysRoleDept;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.mapper.SysRoleDeptMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据权限 实现
 * <p>
 * 注意: 此Service内不允许调用标注`数据权限`注解的方法
 * 例如: deptMapper.selectList 此 selectList 方法标注了`数据权限`注解 会出现循环解析的问题
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteDataScopeServiceImpl implements RemoteDataScopeService {

    private final SysRoleDeptMapper roleDeptMapper;
    private final SysDeptMapper deptMapper;

    @Override
    public String getRoleCustom(Long roleId) {
        List<SysRoleDept> list = roleDeptMapper.selectList(
            new LambdaQueryWrapper<SysRoleDept>()
                .select(SysRoleDept::getDeptId)
                .eq(SysRoleDept::getRoleId, roleId));
        if (CollUtil.isNotEmpty(list)) {
            return list.stream().map(rd -> Convert.toStr(rd.getDeptId())).collect(Collectors.joining(","));
        }
        return null;
    }

    @Override
    public String getDeptAndChild(Long deptId) {
        List<SysDept> deptList = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
            .select(SysDept::getDeptId)
            .apply("find_in_set({0},ancestors) <> 0", deptId));
        List<Long> ids = deptList.stream().map(SysDept::getDeptId).collect(Collectors.toList());
        ids.add(deptId);
        List<SysDept> list = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
            .select(SysDept::getDeptId)
            .in(SysDept::getDeptId, ids));
        if (CollUtil.isNotEmpty(list)) {
            return list.stream().map(d -> Convert.toStr(d.getDeptId())).collect(Collectors.joining(","));
        }
        return null;
    }

}
