package com.sz.disruptor.barrier;

import com.sz.disruptor.producer.ProducerSequencer;
import com.sz.disruptor.sequence.Sequence;
import com.sz.disruptor.strategy.WaitStrategy;

import java.util.Collections;
import java.util.List;

/**
 * @Author
 * @Date 2024-12-21 22:12
 * @Version 1.0
 */
public class SequenceBarrier {

    private final ProducerSequencer producerSequencer;

    private final Sequence currentProducerSequence;

    private final WaitStrategy waitStrategy;

    private final List<Sequence> dependentSequenceList;


    public SequenceBarrier(ProducerSequencer producerSequencer,
                               Sequence currentProducerSequence,
                               WaitStrategy waitStrategy,
                               List<Sequence> dependentSequencesList) {
        this.producerSequencer = producerSequencer;

        this.currentProducerSequence = currentProducerSequence;

        this.waitStrategy = waitStrategy;

        if(!dependentSequencesList.isEmpty()){
            this.dependentSequenceList = dependentSequencesList;
        }else {
            this.dependentSequenceList = Collections.singletonList(currentProducerSequence);
        }

    }

    public long getAvailableConsumerSequence(long currentConsumerSequence) {
        long availableConsumerSequence = waitStrategy.waitFor(currentConsumerSequence, currentProducerSequence, dependentSequenceList);

        if(availableConsumerSequence < currentConsumerSequence){
            return availableConsumerSequence;
        }

        return producerSequencer.getHighestPublishedSequenceNumber(currentConsumerSequence, availableConsumerSequence);
    }
}
