package com.sz.disruptor.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @Author
 * @Date 2024-12-22 21:48
 * @Version 1.0
 */
public class UnsafeUtils {

    private static final Unsafe UNSAFE;

    static {
        try {
            // 由于提供给cas内存中字段偏移量的unsafe类只能在被jdk信任的类中直接使用，这里使用反射来绕过这一限制
            Field getUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            getUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) getUnsafe.get(null);
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Unsafe getUnsafe(){
        return UNSAFE;
    }
}
