package com.yaonan.util.function;

/**
 * 元组
 * Created by yaonan on 2023/9/10.
 */
public class Tuple4<A, B, C, D> {
    /** 第一个元素。 */
    public A _0;
    /** 第二个元素。 */
    public B _1;
    /** 第三个元素。 */
    public C _2;
    /** 第四个元素。 */
    public D _3;

    /**
     * 构造四元组。
     *
     * @param _0 第一个元素
     * @param _1 第二个元素
     * @param _2 第三个元素
     * @param _3 第四个元素
     */
    public Tuple4(A _0, B _1, C _2, D _3) {
        this._0 = _0;
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
    }

    @Override
    public String toString() {
        return "[" + _0 + ", " + _1 + ", " + _2 + ", " + _3 + "]";
    }
}
