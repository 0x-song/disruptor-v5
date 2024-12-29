package com.sz.disruptor.producer;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.sequence.Sequence;

/**
 * @Author
 * @Date 2024-12-21 21:47
 * @Version 1.0
 */
public interface ProducerSequencer {

    long next();

    long next(int n);

    void publish(long publishIndex);

    SequenceBarrier newBarrier();

    SequenceBarrier newBarrier(Sequence... dependenceSequences);

    void addGatingConsumerSequenceList(Sequence... newGatingConsumerSequences);

    Sequence getCurrentConsumerSequence();

    int getRingBufferSize();

    long getHighestPublishedSequenceNumber(long nextSequenceNumber, long availableSequenceNumber);

    Sequence getCurrentProducerSequence();
}
