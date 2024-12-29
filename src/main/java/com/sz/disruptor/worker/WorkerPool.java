package com.sz.disruptor.worker;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.buffer.RingBuffer;
import com.sz.disruptor.sequence.Sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * @Author
 * @Date 2024-12-12 21:03
 * @Version 1.0
 * 多线程消费者
 */
public class WorkerPool<T> {

    private final Sequence workSequence = new Sequence(-1);

    private final RingBuffer<T> ringBuffer;

    private final List<WorkProcessor<T>> workProcessorList;


    public WorkerPool(RingBuffer<T> ringBuffer,
                      SequenceBarrier sequenceBarrier,
                      WorkHandler<T>... workHandlers) {
        this.ringBuffer = ringBuffer;
        final int workerCount = workHandlers.length;
        workProcessorList = new ArrayList<>(workerCount);

        for (WorkHandler<T> eventConsumer : workHandlers) {
            workProcessorList.add(new WorkProcessor<>(
                    ringBuffer,
                    eventConsumer,
                    sequenceBarrier,
                    workSequence
            ));
        }

    }

    //这个方法是做什么呢？？？
    //向外暴露的当前消费者处理的序列，需要让生产者感知到，防止超过一圈
    public Sequence[] getCurrentWorkerSequences(){
        Sequence[] sequences = new Sequence[workProcessorList.size() + 1];
        for (int i = 0; i < workProcessorList.size(); i++) {
            sequences[i] = workProcessorList.get(i).getCurrentConsumerSequence();
        }
        sequences[sequences.length - 1] = workSequence;
        return sequences;
    }

    public RingBuffer<T> start(final Executor executor){
        long cursor = ringBuffer.getCurrentProducerSequencer().get();
        workSequence.set(cursor);

        for (WorkProcessor<T> processor : workProcessorList) {
            processor.getCurrentConsumerSequence().set(cursor);
            executor.execute(processor);
        }
        return ringBuffer;
    }

}
