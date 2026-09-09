package com.yaonan.util.function;

/**
 * 通用函数类型，带异常
 * @see java.util.function.BiFunction
 * Created by yaonan on 2023/4/30.
 */
@FunctionalInterface
public interface BiFunctionE<T, U, R> {
    R apply(T t, U u) throws Exception;
}
