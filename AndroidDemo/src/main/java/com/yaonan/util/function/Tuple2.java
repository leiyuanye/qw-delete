package com.yaonan.util.function;

/**
 * 元组
 * Created by yaonan on 2023/9/10.
 */
public class Tuple2<A, B> {
    /** 第一个元素。 */
    public A _0;
    /** 第二个元素。 */
    public B _1;

    /**
     * 构造二元组。
     *
     * @param _0 第一个元素
     * @param _1 第二个元素
     */
    public Tuple2(A _0, B _1) {
        this._0 = _0;
        this._1 = _1;
    }

    @Override
    public String toString() {
        return "[" + _0 + ", " + _1 + "]";
    }
}
