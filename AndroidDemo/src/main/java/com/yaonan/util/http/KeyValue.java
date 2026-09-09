package com.yaonan.util.http;

/**
 * 键值对对象
 * Created by yaonan on 2023/9/12.
 */
public class KeyValue<K, V> {
    public final K key;

    public final V value;

    public KeyValue(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "{" + key + ": " + value + "}";
    }
}
