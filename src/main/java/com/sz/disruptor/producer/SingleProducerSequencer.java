package com.sz.disruptor.producer;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.sequence.Sequence;
import com.sz.disruptor.strategy.WaitStrategy;
import com.sz.disruptor.util.SequenceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * @Author
 * @Date 2024-12-21 22:16
 * @Version 1.0
 */
public class SingleProducerSequencer implements ProducerSequencer{

    private final int ringBufferSize;

    private final Sequence currentProducerSequence = new Sequence();

    private final List<Sequence> gatingConsumerSequenceList = new ArrayList<Sequence>();

    private final WaitStrategy waitStrategy;

    private long nextValue = -1;

    private long cachedConsumerSequenceValue = -1;

    public SingleProducerSequencer(int ringBufferSize, WaitStrategy waitStrategy) {
        this.ringBufferSize = ringBufferSize;
        this.waitStrategy = waitStrategy;
    }


    @Override
    public long next() {
        return next(1);
    }

    @Override
    public long next(int n) {
        long nextProducerSequence = this.nextValue + n;
        //生产者超过消费者一圈的临界点
        long wrapPoint = nextProducerSequence - ringBufferSize;

        long cachedGatingConsumerSequenceValue = this.cachedConsumerSequenceValue;

        //根据缓存的序列信息判断是不是可能超了
        if(wrapPoint > cachedGatingConsumerSequenceValue){
            long minimumSequence;

            //去加载得到最新的序列信息，是否超了，如果超了，则park
            while (wrapPoint > (minimumSequence = SequenceUtils.getMinimumSequence(nextProducerSequence, gatingConsumerSequenceList))){
                LockSupport.parkNanos(1L);
            }
            //更新缓存的序列
            this.cachedConsumerSequenceValue = minimumSequence;
        }

        this.nextValue = nextProducerSequence;

        return nextProducerSequence;

    }

    @Override
    public void publish(long publishIndex) {
        this.currentProducerSequence.lazySet(publishIndex);

        //唤醒其他阻塞等待的消费者线程
        this.waitStrategy.signalWhenBlocking();
    }

    @Override
    public SequenceBarrier newBarrier() {
        return new SequenceBarrier(this, this.currentProducerSequence, this.waitStrategy, new ArrayList<>());
    }

    @Override
    public SequenceBarrier newBarrier(Sequence... dependenceSequences) {
        return new SequenceBarrier(this, this.currentProducerSequence, this.waitStrategy, new ArrayList<>(Arrays.asList(dependenceSequences)));
    }

    @Override
    public void addGatingConsumerSequenceList(Sequence... newGatingConsumerSequences) {
        this.gatingConsumerSequenceList.addAll(Arrays.asList(newGatingConsumerSequences));
    }

    @Override
    public Sequence getCurrentConsumerSequence() {
        return currentProducerSequence;
    }

    @Override
    public int getRingBufferSize() {
        return ringBufferSize;
    }

    @Override
    public long getHighestPublishedSequenceNumber(long nextSequenceNumber, long availableSequenceNumber) {
        return availableSequenceNumber;
    }

    @Override
    public Sequence getCurrentProducerSequence() {
        return this.currentProducerSequence;
    }
}
