package com.sz.disruptor.event;

/**
 * @Author
 * @Date 2024-12-08 11:18
 * @Version 1.0
 */
public interface EventHandler<T> {

    /**
     *
     * @param event 事件对象本身
     * @param consumerIndex 事件对象在队列里面的序列
     * @param endOfBatch 当前事件是否在这一批处理事件中的最后一个
     */
    void consume(T event, long consumerIndex, boolean endOfBatch);
}
