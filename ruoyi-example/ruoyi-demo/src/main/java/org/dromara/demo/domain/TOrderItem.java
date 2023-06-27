package org.dromara.demo.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("t_order_item")
@Data
public class TOrderItem {

    private Long orderItemId;

    private Long orderId;

    private Long userId;

    private int totalMoney;
}
