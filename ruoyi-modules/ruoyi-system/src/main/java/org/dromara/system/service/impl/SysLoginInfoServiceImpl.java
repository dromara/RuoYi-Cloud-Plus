package org.dromara.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.system.domain.SysLoginInfo;
import org.dromara.system.domain.bo.SysLoginInfoBo;
import org.dromara.system.domain.vo.SysLoginInfoVo;
import org.dromara.system.mapper.SysLoginInfoMapper;
import org.dromara.system.service.ISysLoginInfoService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 系统访问日志情况信息 服务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class SysLoginInfoServiceImpl implements ISysLoginInfoService {

    private final SysLoginInfoMapper loginInfoMapper;

    /**
     * 分页查询登录日志列表
     *
     * @param loginInfo 查询条件
     * @param pageQuery 分页参数
     * @return 登录日志分页列表
     */
    @Override
    public PageResult<SysLoginInfoVo> selectPageLoginInfoList(SysLoginInfoBo loginInfo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysLoginInfo> lqw = buildQueryWrapper(loginInfo);
        if (StringUtils.isBlank(pageQuery.getOrderByColumn())) {
            lqw.orderByDesc(SysLoginInfo::getInfoId);
        }
        Page<SysLoginInfoVo> page = loginInfoMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 新增系统登录日志
     *
     * @param bo 访问日志对象
     */
    @Override
    public void insertLoginInfo(SysLoginInfoBo bo) {
        SysLoginInfo loginInfo = MapstructUtils.convert(bo, SysLoginInfo.class);
        loginInfo.setLoginTime(LocalDateTime.now());
        loginInfoMapper.insert(loginInfo);
    }

    /**
     * 查询系统登录日志集合
     *
     * @param loginInfo 访问日志对象
     * @return 登录记录集合
     */
    @Override
    public List<SysLoginInfoVo> selectLoginInfoList(SysLoginInfoBo loginInfo) {
        return loginInfoMapper.selectVoList(buildQueryWrapper(loginInfo)
            .orderByDesc(SysLoginInfo::getInfoId));
    }

    private LambdaQueryWrapper<SysLoginInfo> buildQueryWrapper(SysLoginInfoBo loginInfo) {
        Map<String, Object> params = loginInfo.getParams();
        return QueryBuilder.lambda(SysLoginInfo.class)
            .likeIfText(SysLoginInfo::getIpaddr, loginInfo.getIpaddr())
            .eqIfText(SysLoginInfo::getStatus, loginInfo.getStatus())
            .eqIfText(SysLoginInfo::getClientKey, loginInfo.getClientKey())
            .eqIfText(SysLoginInfo::getDeviceType, loginInfo.getDeviceType())
            .likeIfText(SysLoginInfo::getUserName, loginInfo.getUserName())
            .betweenParams(SysLoginInfo::getLoginTime, params, "beginTime", "endTime")
            .build();
    }

    /**
     * 批量删除系统登录日志
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    @Override
    public int deleteLoginInfoByIds(Long[] infoIds) {
        return loginInfoMapper.deleteByIds(Arrays.asList(infoIds));
    }

    /**
     * 清空系统登录日志
     */
    @Override
    public void cleanLoginInfo() {
        loginInfoMapper.lambda().delete();
    }
}
