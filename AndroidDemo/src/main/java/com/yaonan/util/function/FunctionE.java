package com.yaonan.util.function;

/**
 * 通用函数类型，带异常
 * @see java.util.function.Function
 * Created by yaonan on 2022/1/8.
 */
@FunctionalInterface
public interface FunctionE<T, R> {
    R apply(T t) throws Exception;
}
