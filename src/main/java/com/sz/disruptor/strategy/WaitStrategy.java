package com.sz.disruptor.strategy;

import com.sz.disruptor.sequence.Sequence;

import java.util.List;

/**
 * @Author
 * @Date 2024-12-22 11:47
 * @Version 1.0
 */
public interface WaitStrategy {
    void signalWhenBlocking();

    long waitFor(long currentConsumerSequenceNumber, Sequence currentProducerSequence, List<Sequence> dependentSequenceList);
}
