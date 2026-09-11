package com.yaonan.util.http;

/**
 * 键值对对象
 * Created by yaonan on 2023/9/12.
 */
public class KeyValue<K, V> {
    /** 键。 */
    public final K key;

    /** 值。 */
    public final V value;

    /**
     * 构造键值对。
     *
     * @param key   键
     * @param value 值
     */
    public KeyValue(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "{" + key + ": " + value + "}";
    }
}
