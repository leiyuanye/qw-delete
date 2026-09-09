package com.yaonan.util.lang;

import static com.yaonan.util.global.Global.TAG;

import android.util.Log;

import com.yaonan.util.exception.ExceptionUtil;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class ThreadUtil {
    public static Thread async(Runnable function) {
        Thread thread = new Thread(function);
        thread.setUncaughtExceptionHandler(new MyUncaughtExceptionHandler());
        thread.start();
        return thread;
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException("sleep interrupted", e);
        }
    }

    public static void sleepInterruptable(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean isInterrupted(boolean clearInterrupted) {
        if (clearInterrupted) {
            return Thread.interrupted();
        } else {
            return Thread.currentThread().isInterrupted();
        }
    }

    public static boolean isInterrupted() {
        return isInterrupted(false);
    }

    public static void startCountDownLatch(int count, Consumer<Integer> function) {
        final CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            Thread thread = new Thread(new CountDownLatchRunnable(i, latch, function));
            thread.start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("startCountDownLatch interrupted", e);
        }
    }

    private static class CountDownLatchRunnable implements Runnable {
        private Integer index;
        private CountDownLatch latch;
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
                latch.countDown();
            }
        }
    }

    static class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Log.e(TAG, "未处理异常" + t);
            ExceptionUtil.getStackTrace(e);
        }
    }
}
