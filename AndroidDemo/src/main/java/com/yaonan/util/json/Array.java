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

    /**
     * 默认构造：创建空数组对象。
     */
    public Array() {
    }

    /**
     * 通过 JSON 数组字符串构造。
     *
     * @param arrayStr JSON 数组字符串
     */
    public Array(String arrayStr) {
        this.arrayStr = arrayStr;
        this.arrayList = Codec.json_decode(arrayStr, List.class);
    }

    /**
     * 通过 List 构造（内部会转换为 JSON 字符串再反解析，保证数据与字符串一致）。
     *
     * @param arrayList 数组数据列表
     */
    public Array(List arrayList) {
        this.arrayStr = Codec.json_encode(arrayList);
        this.arrayList = Codec.json_decode(arrayStr, List.class);
    }

    /**
     * 获取指定索引处的原始元素。
     *
     * @param index 索引
     * @return 原始元素
     */
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

    /**
     * 获取该数组对象对应的 List 形式。
     *
     * @return 底层 List
     */
    public List toList() {
        return arrayList;
    }

    /**
     * 获取数组元素个数。
     *
     * @return 元素个数
     */
    public int size() {
        return arrayList.size();
    }

    /**
     * 返回数组迭代器：嵌套的 List/Map 会被包装为对应的 Array/JSON 对象。
     *
     * @return 迭代器
     */
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
                // 将嵌套集合包装为对应的 Array/JSON 对象，保持类型一致性
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