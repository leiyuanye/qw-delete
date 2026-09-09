package com.yaonan.util.function;

/**
 * 元组
 * Created by yaonan on 2023/9/10.
 */
public class Tuple5<A, B, C, D, E> {
    public A _0;
    public B _1;
    public C _2;
    public D _3;
    public E _4;

    public Tuple5(A _0, B _1, C _2, D _3, E _4) {
        this._0 = _0;
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
        this._4 = _4;
    }

    @Override
    public String toString() {
        return "[" + _0 + ", " + _1 + ", " + _2 + ", " + _3 + ", " + _4 + "]";
    }
}
