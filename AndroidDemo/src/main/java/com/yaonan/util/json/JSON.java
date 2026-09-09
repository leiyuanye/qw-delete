package com.yaonan.util.json;

import com.yaonan.util.codec.Codec;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * JSON解析工具类：JSON对象
 * 目的是三个：
 * 1、弱类型化解析json，比如{"int":5}、{"int":"5"}，json.getInteger("int")都返回整数5，但避免像fastjson那样随意转化类型
 * 2、减少像jackson、gson解析过程中的空指针判断，比如{"ele":null}、{"e":""}，json.getString("ele")都返回null，与Map相同
 * 3、最重要的，想要创建一种像String一样的数据类型！而不是一个工具类！
 *
 * fastjson的逻辑更适用于普通业务场景快速解析json字符串，可惜源码质量还不如小公司写的，基本都是根据阿里业务去写死代码、bug太多、异常处理也不可靠
 * jackson、gson更符合json标准，功能通用性也强，可惜太重了，不适合累业务代码，光是空指针就处理不过来，适合用于底层代码
 * Created by yaonan on 2021/3/8.
 */
public class JSON extends Element {
    /**
     * 该json对象对应字符串
     */
    private String jsonStr;

    /**
     * 该json对象对应字Map
     */
    private Map jsonMap;

    public JSON() {
    }

    public JSON(String jsonStr) {
        this.jsonStr = jsonStr;
        this.jsonMap = Codec.json_decode(jsonStr, Map.class);
    }

    public JSON(Map jsonMap) {
        this.jsonStr = Codec.json_encode(jsonMap);
        this.jsonMap = Codec.json_decode(jsonStr, Map.class);
    }

    private Object get(String key) {
        return jsonMap.get(key);
    }

    /**
     * 获取字符串节点
     * @param key
     * @return
     */
    public String getString(String key) {
        Object value = get(key);
        return super.getString(value);
    }

    /**
     * 获取整数型节点
     * @param key
     * @return
     */
    public Integer getInteger(String key) {
        Object value = get(key);
        return super.getInteger(value);
    }

    /**
     * 获取长整数型节点
     * @param key
     * @return
     */
    public Long getLong(String key) {
        Object value = get(key);
        return super.getLong(value);
    }

    /**
     * 获取大整数型节点
     * @param key
     * @return
     */
    public BigInteger getBigInteger(String key) {
        Object value = get(key);
        return super.getBigInteger(value);
    }

    /**
     * 获取浮点型节点
     * @param key
     * @return
     */
    public Double getDouble(String key) {
        Object value = get(key);
        return super.getDouble(value);
    }

    /**
     * 获取大浮点型节点
     * @param key
     * @return
     */
    public BigDecimal getBigDecimal(String key) {
        Object value = get(key);
        return super.getBigDecimal(value);
    }

    /**
     * 获取布尔节点
     * @param key
     * @return
     */
    public Boolean getBoolean(String key) {
        Object value = get(key);
        return super.getBoolean(value);
    }

    /**
     * 获取时间节点
     * （包括毫秒时间戳）
     * @param key
     * @return
     */
    public Date getDate(String key) {
        Object value = get(key);
        return super.getDate(value);
    }

    /**
     * 获取时间节点
     * （包括毫秒时间戳）
     * @param key
     * @return
     */
    public Timestamp getTimestamp(String key) {
        Object value = get(key);
        return super.getTimestamp(value);
    }

    /**
     * 获取数组节点
     * @param key
     * @return
     */
    public Array getArray(String key) {
        Object value = get(key);
        return super.getArray(value);
    }

    /**
     * 获取对象节点
     * @param key
     * @return
     */
    public JSON getObject(String key) {
        Object value = get(key);
        return super.getObject(value);
    }

    /**
     * @see Codec#escapeJson
     */
    @Override
    public String toString() {
        return jsonStr;
    }

    public Map toMap() {
        return jsonMap;
    }
}
