package com.ruoyi.common.core.web.controller;

import com.ruoyi.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;

/**
 * web层通用数据处理
 *
 * @author ruoyi
 */
@Slf4j
public class BaseController {

    /**
     * 响应返回结果
     *
     * @param rows 影响行数
     * @return 操作结果
     */
    protected R<Void> toAjax(int rows) {
        return rows > 0 ? R.ok() : R.fail();
    }

    /**
     * 响应返回结果
     *
     * @param result 结果
     * @return 操作结果
     */
    protected R<Void> toAjax(boolean result) {
        return result ? success() : error();
    }

    /**
     * 返回成功
     */
    public R<Void> success() {
        return R.ok();
    }

    /**
     * 返回失败消息
     */
    public R<Void> error() {
        return R.fail();
    }

    /**
     * 返回成功消息
     */
    public R<Void> success(String message) {
        return R.ok(message);
    }

    /**
     * 返回失败消息
     */
    public R<Void> error(String message) {
        return R.fail(message);
    }
}
