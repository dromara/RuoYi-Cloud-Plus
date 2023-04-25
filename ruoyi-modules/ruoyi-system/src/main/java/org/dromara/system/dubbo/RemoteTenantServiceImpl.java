package org.dromara.system.dubbo;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.common.core.utils.BeanCopyUtils;
import org.dromara.system.api.RemoteTenantService;
import org.dromara.system.api.domain.vo.RemoteTenantVo;
import org.dromara.system.domain.bo.SysTenantBo;
import org.dromara.system.domain.vo.SysTenantVo;
import org.dromara.system.service.ISysTenantService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zhujie
 */
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteTenantServiceImpl implements RemoteTenantService {

    private final ISysTenantService tenantService;

    /**
     * 根据租户id获取租户详情
     */
    @Override
    public RemoteTenantVo queryByTenantId(String tenantId) {
        SysTenantVo sysTenantVo = tenantService.queryByTenantId(tenantId);
        return BeanCopyUtils.copy(sysTenantVo, RemoteTenantVo.class);
    }

    /**
     * 获取租户列表
     */
    @Override
    public List<RemoteTenantVo> queryList() {
        List<SysTenantVo> sysTenantVos = tenantService.queryList(new SysTenantBo());
        return BeanCopyUtils.copyList(sysTenantVos, RemoteTenantVo.class);
    }

}
