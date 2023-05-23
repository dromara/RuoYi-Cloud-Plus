package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户头像信息
 *
 * @author Michelle.Chung
 */
@Data
public class AvatarVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 头像地址
     */
    private String imgUrl;

}
