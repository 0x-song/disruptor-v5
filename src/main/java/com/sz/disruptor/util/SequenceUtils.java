package com.sz.disruptor.util;

import com.sz.disruptor.sequence.Sequence;

import java.util.List;

/**
 * @Author
 * @Date 2024-12-22 14:57
 * @Version 1.0
 */
public class SequenceUtils {
    public static long getMinimumSequence(long minimumSequenceNumber, List<Sequence> dependentSequenceList) {
        for (Sequence sequence : dependentSequenceList) {
            long value = sequence.get();
            minimumSequenceNumber = Math.min(minimumSequenceNumber, value);
        }
        return minimumSequenceNumber;
    }

    public static long getMinimumSequence(List<Sequence> gatingConsumerSequenceList) {
        return getMinimumSequence(Long.MAX_VALUE, gatingConsumerSequenceList);
    }
}
