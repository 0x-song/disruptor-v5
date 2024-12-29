package com.sz.disruptor.buffer;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.model.factory.EventModelFactory;
import com.sz.disruptor.producer.MultiProducerSequencer;
import com.sz.disruptor.producer.ProducerSequencer;
import com.sz.disruptor.producer.SingleProducerSequencer;
import com.sz.disruptor.sequence.Sequence;
import com.sz.disruptor.strategy.WaitStrategy;

/**
 * @Author
 * @Date 2024-12-10 21:38
 * @Version 1.0
 */
public class RingBuffer<T> {

    private final T[] elementList;

    private final ProducerSequencer producerSequencer;

    private final int ringBufferSize;

    private final int mask;

    private RingBuffer(ProducerSequencer producerSequencer, EventModelFactory<T> eventModelFactory){
        int bufferSize = producerSequencer.getRingBufferSize();
        if(Integer.bitCount(bufferSize) != 1){
            throw new IllegalArgumentException("Buffer size must be a power of 2");
        }
        this.producerSequencer = producerSequencer;
        this.ringBufferSize = bufferSize;
        this.mask = bufferSize - 1;
        this.elementList = (T[]) new Object[ringBufferSize];
        for (int i = 0; i < elementList.length; i++) {
            this.elementList[i] = eventModelFactory.newInstance();
        }
    }

    public T get(long sequence) {
        int index = (int) (sequence & mask);
        return elementList[index];
    }

    public long next(){
        return producerSequencer.next();
    }

    public long next(int n){
        return producerSequencer.next(n);
    }

    public void publish(long index){
        producerSequencer.publish(index);
    }

    public void addGatingConsumerSequence(Sequence... consumerSequence){
        producerSequencer.addGatingConsumerSequenceList(consumerSequence);
    }

    public SequenceBarrier newBarrier(){
        return producerSequencer.newBarrier();
    }

    public SequenceBarrier newBarrier(Sequence... dependentSequences){
        return producerSequencer.newBarrier(dependentSequences);
    }

    public static <E> RingBuffer<E> createSingleProducer(EventModelFactory<E> eventModelFactory, int bufferSize, WaitStrategy waitStrategy){
        SingleProducerSequencer singleProducerSequencer = new SingleProducerSequencer(bufferSize, waitStrategy);
        return new RingBuffer<E>(singleProducerSequencer, eventModelFactory);
    }

    public static <E> RingBuffer<E> createMultiProducer(EventModelFactory<E> eventModelFactory, int bufferSize, WaitStrategy waitStrategy) {
        MultiProducerSequencer sequencer = new MultiProducerSequencer(bufferSize, waitStrategy);
        return new RingBuffer<>(sequencer,eventModelFactory);
    }


    public Sequence getCurrentProducerSequencer() {
        return producerSequencer.getCurrentProducerSequence();
    }
}
