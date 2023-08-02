package org.dromara.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.demo.domain.ShardingOrder;
import org.dromara.demo.mapper.ShardingOrderMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 使用方式 看官网文档扩展项目
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/sharding")
public class TestShardingController {

    private final ShardingOrderMapper torderMapper;

    @GetMapping("/page")
    public R<Page<ShardingOrder>> page() {
        Page<ShardingOrder> page = new Page<>();
        page.setCurrent(3L);
        LambdaQueryWrapper<ShardingOrder> lqw = new LambdaQueryWrapper<>();
        lqw.orderByAsc(ShardingOrder::getOrderId);
        torderMapper.selectPage(page, lqw);
        return R.ok(page);
    }

    @GetMapping("/insert")
    public R<Void> insert() {
        for (Long i = 1L; i <= 100L; i++) {
            ShardingOrder torder = new ShardingOrder();
            torder.setUserId(i);
            torder.setTotalMoney(100 + Integer.parseInt(i + ""));
            torderMapper.insert(torder);
        }

        return R.ok("分库分表数据批量插入成功！");

    }

}
