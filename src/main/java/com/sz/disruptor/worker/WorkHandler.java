package com.sz.disruptor.worker;

/**
 * @Author
 * @Date 2024-12-12 21:11
 * @Version 1.0
 * 多线程消费者处理器接口
 * 单线程消费者处理器接口为EventHandler
 */
public interface WorkHandler<T> {

    /**
     * 消费者消费事件
     * @param event 事件对象本身
     * */
    void consume(T event);
}
