package com.ruoyi.auth.form;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 用户登录对象
 *
 * @author ruoyi
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class LoginBody {

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户密码
     */
    private String password;

}
