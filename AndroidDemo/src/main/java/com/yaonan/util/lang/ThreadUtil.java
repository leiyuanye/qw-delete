package com.yaonan.util.lang;

import static com.yaonan.util.global.Global.TAG;

import android.util.Log;

import com.yaonan.util.exception.ExceptionUtil;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * 线程工具类：提供异步线程创建、线程休眠、中断状态查询与基于 CountDownLatch 的并发执行能力。
 */
public class ThreadUtil {
    /**
     * 异步启动一个线程执行任务，并统一处理未捕获异常。
     *
     * @param function 待执行任务
     * @return 启动的线程
     */
    public static Thread async(Runnable function) {
        Thread thread = new Thread(function);
        thread.setUncaughtExceptionHandler(new MyUncaughtExceptionHandler());
        thread.start();
        return thread;
    }

    /**
     * 使当前线程休眠指定毫秒数（中断时抛出运行时异常）。
     *
     * @param millis 休眠毫秒数
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException("sleep interrupted", e);
        }
    }

    /**
     * 使当前线程休眠指定毫秒数（中断时恢复中断标记，不抛出异常）。
     *
     * @param millis 休眠毫秒数
     */
    public static void sleepInterruptable(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 查询当前线程的中断状态。
     *
     * @param clearInterrupted 是否在查询后清除中断标记
     * @return 是否已中断
     */
    public static boolean isInterrupted(boolean clearInterrupted) {
        if (clearInterrupted) {
            // Thread.interrupted() 会清除中断标记
            return Thread.interrupted();
        } else {
            return Thread.currentThread().isInterrupted();
        }
    }

    /**
     * 查询当前线程的中断状态（不清除中断标记）。
     *
     * @return 是否已中断
     */
    public static boolean isInterrupted() {
        return isInterrupted(false);
    }

    /**
     * 使用 CountDownLatch 并发执行 count 个任务，等待全部完成后返回。
     *
     * @param count    任务数量
     * @param function 消费每个任务索引的处理函数
     */
    public static void startCountDownLatch(int count, Consumer<Integer> function) {
        final CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            Thread thread = new Thread(new CountDownLatchRunnable(i, latch, function));
            thread.start();
        }
        try {
            // 阻塞等待所有子线程完成
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("startCountDownLatch interrupted", e);
        }
    }

    /**
     * CountDownLatch 任务包装类：执行指定索引的任务，并在结束时递减计数器。
     */
    private static class CountDownLatchRunnable implements Runnable {
        /** 任务索引。 */
        private Integer index;
        /** 用于等待所有任务完成的闩锁。 */
        private CountDownLatch latch;
        /** 实际执行的任务函数。 */
        private Consumer<Integer> function;

        private CountDownLatchRunnable(Integer index, CountDownLatch latch, Consumer<Integer> function) {
            this.index = index;
            this.latch = latch;
            this.function = function;
        }

        @Override
        public void run() {
            try {
                function.accept(index);
            } catch (Exception e) {
                new MyUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            } finally {
                // 无论成功与否都递减计数器，避免主线程永久阻塞
                latch.countDown();
            }
        }
    }

    /**
     * 未捕获异常处理器：记录异常日志。
     */
    static class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Log.e(TAG, "未处理异常" + t);
            ExceptionUtil.getStackTrace(e);
        }
    }
}
