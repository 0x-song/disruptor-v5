package com.sz.disruptor.processor;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.buffer.RingBuffer;
import com.sz.disruptor.event.EventHandler;
import com.sz.disruptor.sequence.Sequence;



/**
 * @Author
 * @Date 2024-12-10 22:38
 * @Version 1.0
 */
public class BatchEventProcessor<T> implements Runnable{

    private final Sequence currentConsumerSequence = new Sequence(-1);

    private final RingBuffer<T> ringBuffer;

    private final EventHandler consumerHandler;

    private final SequenceBarrier sequenceBarrier;

    public BatchEventProcessor(RingBuffer<T> ringBuffer, EventHandler consumerHandler, SequenceBarrier sequenceBarrier) {
        this.ringBuffer = ringBuffer;
        this.consumerHandler = consumerHandler;
        this.sequenceBarrier = sequenceBarrier;
    }

    @Override
    public void run() {
        long nextConsumerSequence = currentConsumerSequence.get() + 1;

        while (true){
            long availableConsumerSequence = sequenceBarrier.getAvailableConsumerSequence(nextConsumerSequence);

            while (nextConsumerSequence <= availableConsumerSequence){
                T event = ringBuffer.get(nextConsumerSequence);
                consumerHandler.consume(event, nextConsumerSequence, nextConsumerSequence == availableConsumerSequence);

                nextConsumerSequence ++;
            }
            currentConsumerSequence.lazySet(availableConsumerSequence);
            System.out.println("更新消费者序列:" + availableConsumerSequence);
        }
    }

    public Sequence getCurrentConsumerSequence() {
        return currentConsumerSequence;
    }
}
