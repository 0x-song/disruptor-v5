package com.sz.disruptor.worker;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.buffer.RingBuffer;
import com.sz.disruptor.sequence.Sequence;

/**
 * @Author
 * @Date 2024-12-12 21:07
 * @Version 1.0
 * 多线程消费者
 *  某一个消费者会使用多个线程来处理对应序列的事件
 *  当前类便是线程处理的逻辑
 */
public class WorkProcessor<T> implements Runnable {

    private final Sequence currentConsumerSequence = new Sequence(-1);

    private final RingBuffer<T> ringBuffer;

    private final WorkHandler<T> workHandler;

    private final SequenceBarrier sequenceBarrier;

    private final Sequence workGroupSequence;


    public WorkProcessor(RingBuffer<T> ringBuffer,
                         WorkHandler<T> eventConsumer,
                         SequenceBarrier sequenceBarrier,
                         Sequence workGroupSequence) {
        this.ringBuffer = ringBuffer;
        this.workHandler = eventConsumer;
        this.sequenceBarrier = sequenceBarrier;
        this.workGroupSequence = workGroupSequence;
    }

    public Sequence getCurrentConsumerSequence() {
        return currentConsumerSequence;
    }

    @Override
    public void run() {
        //模拟刚开始的场景： -1 + 1 = 0
        long nextConsumerSequence = currentConsumerSequence.get() + 1;
        long cachedAvailableSequence = Long.MIN_VALUE;
        boolean processedSequence = true;

        while (true){
            System.out.println("currentConsumerSequence = " + currentConsumerSequence.get() + "workGroupSequence = " + workGroupSequence.get() );
            if(processedSequence){
                //走到这个分支会进入到循环中，如果能够循环出来，说明抢到了一个序列
                processedSequence = false;
                do{
                    nextConsumerSequence = this.workGroupSequence.get() + 1L;
                    this.currentConsumerSequence.lazySet(nextConsumerSequence - 1L);

                }while (!workGroupSequence.compareAndSet(nextConsumerSequence - 1L, nextConsumerSequence));
            }
            /*else {
                //走到这里，表示刚刚抢到一个序列，还有一个序列等待去消费
            }*/
            if(nextConsumerSequence <= cachedAvailableSequence){
                //第一次进来时，绝对会进入到else分支中
                //抢到的序列小于缓存的序列，则说明可以进行消费，是安全的
                T event = ringBuffer.get(nextConsumerSequence);
                this.workHandler.consume(event);

                //下一次又可以去抢夺了
                processedSequence = true;
            }else {
                cachedAvailableSequence = sequenceBarrier.getAvailableConsumerSequence(nextConsumerSequence);
            }

        }

    }
}
