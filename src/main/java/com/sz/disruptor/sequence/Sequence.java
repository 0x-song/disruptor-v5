package com.sz.disruptor.sequence;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @Author
 * @Date 2024-12-21 21:42
 * @Version 1.0
 */
public class Sequence {

    private volatile long value = -1;

    private static final Unsafe unsafe;

    private static final long value_offset;

    static {
        Field getUnsafe = null;
        try {
            getUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            getUnsafe.setAccessible(true);

            unsafe = (Unsafe) getUnsafe.get(null);
            value_offset = unsafe.objectFieldOffset(Sequence.class.getDeclaredField("value"));

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Sequence(long value) {
        this.value = value;
    }

    public Sequence() {
    }

    public long get() {
        return value;
    }

    public void set(long value) {
        this.value = value;
    }

    public void lazySet(long value) {
        unsafe.putOrderedLong(this, value_offset, value);
    }

    public boolean compareAndSet(long expect, long update) {
        return unsafe.compareAndSwapLong(this, value_offset, expect, update);
    }
}
