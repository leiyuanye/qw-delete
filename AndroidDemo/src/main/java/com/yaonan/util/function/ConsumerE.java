package com.yaonan.util.function;

/**
 * 通用函数类型，带异常
 * @see java.util.function.Consumer
 * Created by yaonan on 2022/1/8.
 */
@FunctionalInterface
public interface ConsumerE<T> {
    void accept(T t) throws Exception;
}
