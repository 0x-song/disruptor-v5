package com.sz.disruptor.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * @Author
 * @Date 2024-12-10 21:23
 * @Version 1.0
 */
public class ThreadUtils {

    private static final MethodHandle on_spin_wait_method_handler;


    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle methodHandle = null;
        try {
            methodHandle = lookup.findStatic(Thread.class, "onSpinWait", MethodType.methodType(void.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        on_spin_wait_method_handler = methodHandle;

    }

    public static void onSpinWait() {
        if(null != on_spin_wait_method_handler) {
            try {
                on_spin_wait_method_handler.invokeExact();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
