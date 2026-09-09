package com.yaonan.util.function;

/**
 * 通用函数类型，带异常
 * @see java.util.function.Supplier
 * Created by yaonan on 2022/1/8.
 */
@FunctionalInterface
public interface SupplierE<T> {
    T get() throws Exception;
}
