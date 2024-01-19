package org.dromara.auth.domain.vo;

import lombok.Data;

/**
 * 租户列表
 *
 * @author zhujie
 */
@Data
public class TenantListVo {

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 企业名称
     */
    private String companyName;

    /**
     * 域名
     */
    private String domain;

}
