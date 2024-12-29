package com.sz.disruptor.model.factory;

import com.sz.disruptor.model.OrderEventModel;

/**
 * @Author
 * @Date 2024-12-08 15:30
 * @Version 1.0
 */
public class OrderEventModelFactory implements EventModelFactory<OrderEventModel> {
    @Override
    public OrderEventModel newInstance() {
        return new OrderEventModel();
    }
}
