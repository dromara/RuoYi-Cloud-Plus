package org.dromara.auth.domain.vo;

import lombok.Data;

/**
 * 租户列表
 *
 * @author zhujie
 */
@Data
public class TenantListVo {

    private String tenantId;

    private String companyName;

    private String domain;

}
