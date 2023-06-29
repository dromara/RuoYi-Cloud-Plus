package org.dromara.demo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.dromara.demo.domain.ShardingOrder;
import org.dromara.demo.mapper.ShardingOrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TOrderTest {

    @Autowired
    ShardingOrderMapper torderMapper;


    @Test
    void find() {
        //Order order = orderMapper.selectById(1640990702722723841L);
    }

    @Test
    void page() {
        Page<ShardingOrder> page = new Page<>();
        page.setCurrent(3L);
        QueryWrapper<ShardingOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("order_id");
        torderMapper.selectPage(page,queryWrapper);
        System.out.println(page.getTotal());
        for(ShardingOrder order : page.getRecords()){
            System.out.print(order.getTotalMoney()+" ");
        }
    }

    @Test
    void insert() {
        for(Long i = 1L; i <= 100L; i++){
            ShardingOrder torder = new ShardingOrder();
            torder.setUserId(i);
            torder.setTotalMoney(100 + Integer.parseInt(i+""));
            torderMapper.insert(torder);
        }

    }


}
