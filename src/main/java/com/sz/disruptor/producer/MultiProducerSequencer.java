package com.sz.disruptor.producer;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.sequence.Sequence;
import com.sz.disruptor.strategy.WaitStrategy;
import com.sz.disruptor.util.SequenceUtils;
import com.sz.disruptor.util.UnsafeUtils;
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * @Author
 * @Date 2024-12-21 22:16
 * @Version 1.0
 * 多线程生产者
 */
public class MultiProducerSequencer implements ProducerSequencer{

    private final int ringBufferSize;

    private final Sequence currentProducerSequence = new Sequence();

    private final List<Sequence> gatingConsumerSequenceList = new ArrayList<>();

    private final WaitStrategy waitStrategy;

    private final Sequence gatingSequenceCache = new Sequence();

    private final int[] availableBuffer;

    //用于快速进行取余运算
    private final int indexMask;

    //用来确定序号在环形缓冲区中"转了多少圈"。
    /**
     * position = 11
     * // 二进制：1011
     * index = 11 & 7 = 3    // 在环中的位置
     * sequence = 11 >> 3 = 1 // 转了1圈
     *
     * position = 3
     * // 二进制：0011
     * index = 3 & 7 = 3     // 在环中的位置
     * sequence = 3 >> 3 = 0  // 还没转完一圈
     */
    private final int indexShift;

    private static final Unsafe unsafe = UnsafeUtils.getUnsafe();

    //base表示数组中第一个元素的起始位置
    private static final long base = unsafe.arrayBaseOffset(int[].class);

    //scale表示数组中每个元素占用的字节数
    private static final long scale = unsafe.arrayIndexScale(int[].class);

    public MultiProducerSequencer(int ringBufferSize, WaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
        this.ringBufferSize = ringBufferSize;
        this.availableBuffer = new int[ringBufferSize];
        this.indexMask = ringBufferSize - 1;
        this.indexShift = log2(ringBufferSize);
        initAvailableBuffer();
    }

    //availableBuffer内部的值都设置为-1的初始值
    //availableBuffer中的值标识的是ringBuffer中对应下标位置的事件第几次被覆盖。
    //举个例子：一个长度为8的ringBuffer，其内部数组下标为2的位置，当序列号为2时其值会被设置为0（第一次被设置值，未被覆盖），
    // 序列号为10时其值会被设置为1（被覆盖一次），序列号为18时其值会被设置为2（被覆盖两次），以此类推。
    //序列号对应的下标值通过calculateIndex求模运算获得，
    // 而被覆盖的次数通过calculateAvailabilityFlag方法对当前发布的序列号做对数计算出来。
    private void initAvailableBuffer() {
        for (int i = availableBuffer.length - 1; i >= 0; i--) {
            this.availableBuffer[i] = -1;
        }
    }

    //i右移直到变成0 log2(8) = 3
    private static int log2(int i) {
        int r = 0;
        while ((i >>= 1) != 0) {
            ++r;
        }
        return r;
    }

    @Override
    public long next() {
        return next(1);
    }

    @Override
    public long next(int n) {
        do {
            long currentMaxProducerSequenceNumber = currentProducerSequence.get();

            long nextProducerSequenceNumber = currentMaxProducerSequenceNumber + n;

            //生产者超过消费者一圈的临界点序列
            long wrapPoint = nextProducerSequenceNumber - this.ringBufferSize;

            long cachedGatingSequence = this.gatingSequenceCache.get();

            if(wrapPoint > cachedGatingSequence){
                long gatingSequence = SequenceUtils.getMinimumSequence(currentMaxProducerSequenceNumber, this.gatingConsumerSequenceList);

                if(wrapPoint > gatingSequence){
                    LockSupport.parkNanos(1L);

                    //park短暂阻塞之后continue跳出重新进入循环
                    continue;
                }

                //更新一下缓存的序列
                this.gatingSequenceCache.set(gatingSequence);
            }else {
                //生产者没有超过消费者一圈，可以放心生产
                if(this.currentProducerSequence.compareAndSet(currentMaxProducerSequenceNumber, nextProducerSequenceNumber)){
                    return nextProducerSequenceNumber;
                }
                //cas失败，则重新进入循环中获取最新的消费序列
            }
        }while (true);
    }

    //在MultiProducerSequencer的publish方法中，通过setAvailable来标示当前序号为已发布的状态
    @Override
    public void publish(long publishIndex) {
        setAvailable(publishIndex);
        waitStrategy.signalWhenBlocking();
    }

    //将特定序列标记为可供消费者使用
    //消费者会调用isAvailable来进行比对，如果一致，可以消费；如果不一致，不可以消费
    //还未完全搞清楚原理
    private void setAvailable(long publishIndex) {
        int index = calculateIndex(publishIndex);
        int flag = calculateAvailabilityFlag(publishIndex);

        // 计算index对应下标相对于availableBuffer引用起始位置的指针偏移量
        long bufferAddress = (index * scale) + base;

        // 功能上等价于this.availableBuffer[index] = flag，但添加了写屏障
        // 和单线程生产者中的lazySet作用一样，保证了对publish发布的event事件对象的更新一定先于对availableBuffer对应下标值的更新
        // 避免消费者拿到新的发布序列号时由于新event事件未对其可见，而错误的消费了之前老的event事件
        unsafe.putOrderedInt(availableBuffer, bufferAddress, flag);

    }

    private int calculateAvailabilityFlag(long publishIndex) {

        return (int) (publishIndex >>> indexShift);
    }

    private int calculateIndex(long publishIndex) {

        return ((int) publishIndex) & indexMask;
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
        return this.currentProducerSequence;
    }

    @Override
    public int getRingBufferSize() {
        return this.ringBufferSize;
    }

    @Override
    public long getHighestPublishedSequenceNumber(long lowBound, long availableSequenceNumber) {
        // lowBound是消费者传入的，保证是已经明确发布了的最小生产者序列号
        // 因此，从lowBound开始，向后寻找,有两种情况
        // 1 在lowBound到availableSequence中间存在未发布的下标(isAvailable(sequence) == false)，
        // 那么，找到的这个未发布下标的前一个序列号，就是当前最大的已经发布了的序列号（可以被消费者正常消费）
        // 2 在lowBound到availableSequence中间不存在未发布的下标，那么就和单生产者的情况一样
        // 包括availableSequence以及之前的序列号都已经发布过了，availableSequence就是当前可用的最大的的序列号（已发布的）
        for(long sequence = lowBound; sequence <= availableSequenceNumber; sequence++){
            if (!isAvailable(sequence)) {
                // 属于上述的情况1，lowBound和availableSequence中间存在未发布的序列号
                return sequence - 1;
            }
        }
        return availableSequenceNumber;
    }

    @Override
    public Sequence getCurrentProducerSequence() {
        return this.currentProducerSequence;
    }

    //在消费者序列屏障中被调用的getHighestPublishedSequence方法中，则通过isAvailable来判断传入的序列号是否已发布
    private boolean isAvailable(long sequence) {
        int index = calculateIndex(sequence);
        int flag = calculateAvailabilityFlag(sequence);

        // 计算index对应下标相对于availableBuffer引用起始位置的指针偏移量
        long bufferAddress = (index * scale) + base;

        // 功能上等价于this.availableBuffer[index] == flag
        // 但是添加了读屏障保证了强一致的读，可以让消费者实时的获取到生产者新的发布
        return unsafe.getIntVolatile(availableBuffer, bufferAddress) == flag;
    }
}
