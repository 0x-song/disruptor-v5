package com.sz.disruptor.model.factory;

/**
 * @Author
 * @Date 2024-12-08 15:30
 * @Version 1.0
 */
public interface EventModelFactory<T> {

    T newInstance();
}
