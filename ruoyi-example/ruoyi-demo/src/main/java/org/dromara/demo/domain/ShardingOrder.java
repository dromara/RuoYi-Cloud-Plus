package org.dromara.demo.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("t_order")
@Data
public class ShardingOrder {


    private Long orderId;

    private Long userId;

    private int totalMoney;
}
