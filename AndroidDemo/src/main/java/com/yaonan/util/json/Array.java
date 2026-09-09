package com.yaonan.util.json;

import com.yaonan.util.codec.Codec;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JSON解析工具类：数组对象
 * Created by yaonan on 2021/3/8.
 */
public class Array extends Element implements Iterable {
    /**
     * 该array对象对应字符串
     */
    private String arrayStr;

    /**
     * 该array对象对应字List
     */
    private List arrayList;

    public Array() {
    }

    public Array(String arrayStr) {
        this.arrayStr = arrayStr;
        this.arrayList = Codec.json_decode(arrayStr, List.class);
    }

    public Array(List arrayList) {
        this.arrayStr = Codec.json_encode(arrayList);
        this.arrayList = Codec.json_decode(arrayStr, List.class);
    }

    private Object get(int index) {
        return arrayList.get(index);
    }

    /**
     * 根据索引获取字符串元素
     * @param index
     * @return
     */
    public String getString(int index) {
        Object value = get(index);
        return super.getString(value);
    }

    /**
     * 根据索引获取整数型元素
     * @param index
     * @return
     */
    public Integer getInteger(int index) {
        Object value = get(index);
        return super.getInteger(value);
    }

    /**
     * 根据索引获取长整数型元素
     * @param index
     * @return
     */
    public Long getLong(int index) {
        Object value = get(index);
        return super.getLong(value);
    }

    /**
     * 根据索引获取大整数型元素
     * @param index
     * @return
     */
    public BigInteger getBigInteger(int index) {
        Object value = get(index);
        return super.getBigInteger(value);
    }

    /**
     * 根据索引获取浮点型元素
     * @param index
     * @return
     */
    public Double getDouble(int index) {
        Object value = get(index);
        return super.getDouble(value);
    }

    /**
     * 根据索引获取大浮点型元素
     * @param index
     * @return
     */
    public BigDecimal getBigDecimal(int index) {
        Object value = get(index);
        return super.getBigDecimal(value);
    }

    /**
     * 根据索引获取布尔元素
     * @param index
     * @return
     */
    public Boolean getBoolean(int index) {
        Object value = get(index);
        return super.getBoolean(value);
    }

    /**
     * 根据索引获取时间元素
     * （包括毫秒时间戳）
     * @param index
     * @return
     */
    public Date getDate(int index) {
        Object value = get(index);
        return super.getDate(value);
    }

    /**
     * 根据索引获取时间元素
     * （包括毫秒时间戳）
     * @param index
     * @return
     */
    public Timestamp getTimestamp(int index) {
        Object value = get(index);
        return super.getTimestamp(value);
    }

    /**
     * 根据索引获取数组元素
     * @param index
     * @return
     */
    public Array getArray(int index) {
        Object value = get(index);
        return super.getArray(value);
    }

    /**
     * 根据索引获取对象元素
     * @param index
     * @return
     */
    public JSON getObject(int index) {
        Object value = get(index);
        return super.getObject(value);
    }

    @Override
    public String toString() {
        return arrayStr;
    }

    public List toList() {
        return arrayList;
    }

    public int size() {
        return arrayList.size();
    }

    @Override
    public Iterator iterator() {
        class ArrayIterator implements Iterator {
            private List _a;

            private int _index;

            private ArrayIterator(List a) {
                _a = a;
                _index = 0;
            }

            @Override
            public boolean hasNext() {
                return _index < _a.size();
            }

            @Override
            public Object next() {
                Object value = _a.get(_index++);
                if (value instanceof List) {
                    return new Array((List) value);
                } else if (value instanceof Map) {
                    return new JSON((Map) value);
                } else {
                    return value;
                }
            }
        }
        return new ArrayIterator(arrayList);
    }
}