package com.yaonan.util.function;

/**
 * 通用函数类型，带异常
 * @see java.util.function.BiConsumer
 * Created by yaonan on 2023/5/6.
 */
@FunctionalInterface
public interface BiConsumerE<T, U> {
    void accept(T t, U u) throws Exception;
}
