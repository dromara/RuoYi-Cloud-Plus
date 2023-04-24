package org.dromara.system.api;

import org.dromara.common.core.exception.user.UserException;
import org.dromara.system.api.domain.bo.RemoteUserBo;
import org.dromara.system.api.model.LoginUser;
import org.dromara.system.api.model.XcxLoginUser;

/**
 * 用户服务
 *
 * @author Lion Li
 */
public interface RemoteUserService {

    /**
     * 通过用户名查询用户信息
     *
     * @param username 用户名
     * @return 结果
     */
    LoginUser getUserInfo(String username) throws UserException;

    /**
     * 通过手机号查询用户信息
     *
     * @param phonenumber 手机号
     * @return 结果
     */
    LoginUser getUserInfoByPhonenumber(String phonenumber) throws UserException;

    /**
     * 通过邮箱查询用户信息
     *
     * @param email 邮箱
     * @return 结果
     */
    LoginUser getUserInfoByEmail(String email) throws UserException;

    /**
     * 通过openid查询用户信息
     *
     * @param openid openid
     * @return 结果
     */
    XcxLoginUser getUserInfoByOpenid(String openid) throws UserException;

    /**
     * 注册用户信息
     *
     * @param remoteUserBo 用户信息
     * @return 结果
     */
    Boolean registerUserInfo(RemoteUserBo remoteUserBo);

    /**
     * 通过userId查询用户账户
     *
     * @param userId 用户id
     * @return 结果
     */
    String selectUserNameById(Long userId);
}
