package com.ruoyi.common.core.annotation;

import java.lang.annotation.*;

/**
 * dubbo 内网鉴权放行
 *
 * @author Lion Li
 */
@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InnerExclude {
}
