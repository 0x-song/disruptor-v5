package com.sz.disruptor;


import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.buffer.RingBuffer;
import com.sz.disruptor.event.OrderEventHandler;
import com.sz.disruptor.model.OrderEventModel;
import com.sz.disruptor.model.factory.OrderEventModelFactory;
import com.sz.disruptor.processor.BatchEventProcessor;
import com.sz.disruptor.sequence.Sequence;
import com.sz.disruptor.strategy.BlockingWaitStrategy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DisruptorV5Main {
    public static void main(String[] args) {
        int ringBufferSize = 16;

        //创建环形队列
        RingBuffer<OrderEventModel> ringBuffer = RingBuffer.createMultiProducer(new OrderEventModelFactory(), ringBufferSize, new BlockingWaitStrategy());

        //最上游的序列屏障中只有生产者
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

        BatchEventProcessor<OrderEventModel> eventProcessorA = new BatchEventProcessor<>(ringBuffer, new OrderEventHandler("consumerA"), sequenceBarrier);

        Sequence consumerSequenceA = eventProcessorA.getCurrentConsumerSequence();

        ringBuffer.addGatingConsumerSequence(consumerSequenceA);

        new Thread(eventProcessorA).start();

        //启动多线程生产者
        ExecutorService executorService = Executors.newFixedThreadPool(10, new ThreadFactory() {

            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {

                return new Thread(r, "workerProducer" + mCount.getAndIncrement());
            }
        });

        for (int i = 0; i < 3; i++) {
            int finalI = i;
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 100; j++) {
                        long nextIndex = ringBuffer.next();
                        OrderEventModel orderEvent = ringBuffer.get(nextIndex);
                        orderEvent.setMessage("message-" + finalI + "-" + j);
                        orderEvent.setPrice(finalI * j * 10);
                        System.out.println(nextIndex + ":" + orderEvent);
                        ringBuffer.publish(nextIndex);
                    }
                }
            });
        }
    }
}