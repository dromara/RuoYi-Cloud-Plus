package org.dromara.common.doc.config;

import io.swagger.v3.oas.models.Paths;

/**
 * 单独使用一个类便于判断 解决springdoc路径拼接重复问题
 *
 * @author Lion Li
 */
public class PlusPaths extends Paths {

    public PlusPaths() {
        super();
    }
}
