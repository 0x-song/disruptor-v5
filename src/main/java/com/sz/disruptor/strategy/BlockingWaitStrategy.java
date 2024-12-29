package com.sz.disruptor.strategy;

import com.sz.disruptor.sequence.Sequence;
import com.sz.disruptor.util.SequenceUtils;
import com.sz.disruptor.util.ThreadUtils;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author
 * @Date 2024-12-22 20:44
 * @Version 1.0
 */
public class BlockingWaitStrategy implements WaitStrategy {

    private final Lock lock = new ReentrantLock();

    private final Condition processorNotifyCondition = lock.newCondition();

    @Override
    public void signalWhenBlocking() {
        lock.lock();
        processorNotifyCondition.signalAll();
        lock.unlock();
    }

    @Override
    public long waitFor(long currentConsumerSequenceNumber, Sequence currentProducerSequence, List<Sequence> dependentSequenceList) {

        if(currentProducerSequence.get() < currentConsumerSequenceNumber){
            lock.lock();
            try {
                while (currentProducerSequence.get() < currentConsumerSequenceNumber) {
                    processorNotifyCondition.await();
                }
            }catch (Exception e){
            }finally {
                lock.unlock();
            }
        }

        //此时生产者序列超过了消费者
        long availableSequenceNumber;
        if(!dependentSequenceList.isEmpty()){
            while ((availableSequenceNumber = SequenceUtils.getMinimumSequence(dependentSequenceList)) < currentConsumerSequenceNumber){
                ThreadUtils.onSpinWait();
            }
        }else {
            availableSequenceNumber =currentProducerSequence.get();
        }
        return availableSequenceNumber;
    }
}
