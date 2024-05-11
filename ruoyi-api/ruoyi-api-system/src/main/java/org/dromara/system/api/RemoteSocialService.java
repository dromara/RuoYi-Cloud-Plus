package org.dromara.system.api;

import org.dromara.system.api.domain.bo.RemoteSocialBo;
import org.dromara.system.api.domain.vo.RemoteSocialVo;

import java.util.List;

/**
 * 社会化关系服务
 *
 * @author Michelle.Chung
 */
public interface RemoteSocialService {

    /**
     * 根据 authId 查询用户授权信息
     *
     * @param authId 认证id
     * @return 授权信息
     */
    List<RemoteSocialVo> selectByAuthId(String authId);

    /**
     * 查询列表
     *
     * @param bo 社会化关系业务对象
     */
    List<RemoteSocialVo> queryList(RemoteSocialBo bo);

    /**
     * 保存社会化关系
     *
     * @param bo 社会化关系业务对象
     */
    void insertByBo(RemoteSocialBo bo);

    /**
     * 更新社会化关系
     *
     * @param bo 社会化关系业务对象
     */
    void updateByBo(RemoteSocialBo bo);

    /**
     * 删除社会化关系
     *
     * @param socialId 社会化关系ID
     * @return 结果
     */
    Boolean deleteWithValidById(Long socialId);

}
