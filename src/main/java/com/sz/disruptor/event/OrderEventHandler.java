package com.sz.disruptor.event;

import com.sz.disruptor.model.OrderEventModel;

/**
 * @Author
 * @Date 2024-12-08 15:32
 * @Version 1.0
 * 订单事件处理器
 */
public class OrderEventHandler implements EventHandler<OrderEventModel>{

    private String name;

    public OrderEventHandler(String name) {
        this.name = name;
    }

    @Override
    public void consume(OrderEventModel event, long sequence, boolean endOfBatch) {
//        if(name.equals("consumerA")){
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
        System.out.println(name + "消费事件" + event + " com.sz.disruptor.sequence=" + sequence + " endOfBatch=" + endOfBatch);
    }
}
