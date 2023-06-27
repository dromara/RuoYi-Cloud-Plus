package org.dromara.demo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.dromara.demo.domain.TOrder;
import org.dromara.demo.mapper.TOrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TOrderTest {

    @Autowired
    TOrderMapper torderMapper;

    //@Autowired
    //TUserMapper userMapper;

    @Test
    void find() {
        //Order order = orderMapper.selectById(1640990702722723841L);
    }

    @Test
    void page() {
        Page<TOrder> page = new Page<>();
        page.setCurrent(3L);
        QueryWrapper<TOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("order_id");
        torderMapper.selectPage(page,queryWrapper);
        System.out.println(page.getTotal());
        for(TOrder order : page.getRecords()){
            System.out.print(order.getTotalMoney()+" ");
        }
    }

    @Test
    void insert() {
        for(Long i = 1L; i <= 100L; i++){
            TOrder torder = new TOrder();
            torder.setUserId(i);
            torder.setTotalMoney(100 + Integer.parseInt(i+""));
            torderMapper.insert(torder);
        }

    }
    @Test
    void insertUser() {
        /*User user = new User();
        user.setId(1L);
        user.setUserName("abc");
        user.setAge(18);
        user.setCreateTime(new Date());
        userMapper.insert(user);*/
    }

}
