package com.yaonan.util.function;

/**
 * 通用函数类型，带异常
 * @see Runnable
 * Created by yaonan on 2022/1/8.
 */
@FunctionalInterface
public interface RunnableE {
    void run() throws Exception;
}
