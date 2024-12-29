package com.sz.disruptor.worker;

import com.sz.disruptor.model.OrderEventModel;

/**
 * @Author
 * @Date 2024-12-18 21:52
 * @Version 1.0
 */
public class OrderWorkHandler implements WorkHandler<OrderEventModel>{

    private String consumerName;

    public OrderWorkHandler(String consumerName) {
        this.consumerName = consumerName;
    }


    @Override
    public void consume(OrderEventModel event) {
        System.out.println(consumerName + " work组消费者消费事件" + event);
    }
}
