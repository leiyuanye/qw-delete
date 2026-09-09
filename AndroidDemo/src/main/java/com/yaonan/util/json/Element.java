package com.yaonan.util.json;

import com.yaonan.util.lang.TimeUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JSON解析工具类：元素类型转换方法
 * |-----------------------------------------------------------------------------------------------------|
 * | 包含元素类型 | STRING | BOOLEAN | OBJECT        | ARRAY     | NUMBER                                |
 * |-----------------------------------------------------------------------------------------------------|
 * | jackson      | String | Boolean | LinkedHashMap | ArrayList | Integer, Long, BigInteger, Double     |
 * | gson         | String | Boolean | LinkedTreeMap | ArrayList | Double                                |
 * | fastjson     | String | Boolean | JSONObject    | JSONArray | Integer, Long, BigInteger, BigDecimal |
 * |-----------------------------------------------------------------------------------------------------|
 *
 * Created by yaonan on 2021/3/8.
 */
public class Element {
    /**
     * 转换字符串元素
     * @param value
     * @return
     */
    protected String getString(Object value) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Map || value instanceof List)) {
            return value.toString();
        }

        throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + String.class.getName());
    }

    /**
     * 转换整数型元素
     * @param value
     * @return
     */
    protected Integer getInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof String) {
            if ("".equals(value)) {
                return null;
            }
            return Integer.parseInt((String) value);
        }

        throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + Integer.class.getName());
    }

    /**
     * 转换长整数型元素
     * @param value
     * @return
     */
    protected Long getLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof String) {
            if ("".equals(value)) {
                return null;
            }
            return Long.parseLong((String) value);
        }

        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + Long.class.getName());
    }

    /**
     * 转换大整数型元素
     * @param value
     * @return
     */
    protected BigInteger getBigInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigInteger) {
            return (BigInteger) value;
        }

        if (value instanceof String) {
            if ("".equals(value)) {
                return null;
            }
            return new BigInteger((String) value);
        }

        if (value instanceof Integer) {
            return BigInteger.valueOf((Integer) value);
        }

        if (value instanceof Long) {
            return BigInteger.valueOf((Long) value);
        }

        throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + BigInteger.class.getName());
    }

    /**
     * 转换浮点型元素
     * @param value
     * @return
     */
    protected Double getDouble(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Double) {
            // 可能是Infinity
            return (Double) value;
        }

        if (value instanceof String) {
            if ("".equals(value)) {
                return null;
            }
            return Double.parseDouble((String) value);
        }

        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }

        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }

        if (value instanceof BigInteger) {
            // 可能是Infinity
            return ((BigInteger) value).doubleValue();
        }

        if (value instanceof BigDecimal) {
            // 可能是Infinity
            return ((BigDecimal) value).doubleValue();
        }

        throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + Double.class.getName());
    }

    /**
     * 转换大浮点型元素
     * @param value
     * @return
     */
    protected BigDecimal getBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof String) {
            if ("".equals(value)) {
                return null;
            }
            return new BigDecimal((String) value);
        }

        if (value instanceof Integer) {
            return BigDecimal.valueOf((Integer) value);
        }

        if (value instanceof Long) {
            return BigDecimal.valueOf((Long) value);
        }

        if (value instanceof BigInteger) {
            return new BigDecimal((BigInteger) value);
        }

        if (value instanceof Double) {
            // 可能是Infinity
            return BigDecimal.valueOf((Double) value);
        }

        throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + BigDecimal.class.getName());
    }

    /**
     * 转换布尔元素
     * @param value
     * @return
     */
    protected Boolean getBoolean(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof String) {
            if ("".equals(value)) {
                return null;
            } else if ("true".equals(value)) {
                return true;
            } else if  ("false".equals(value)) {
                return false;
            }

            throw new ClassCastException("'" + value + "' cannot be cast to " + Boolean.class.getName() + ", value : ");
        }

        throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + Boolean.class.getName());
    }

    /**
     * 转换时间元素
     * （包括毫秒时间戳）
     * @param value
     * @return
     */
    protected Date getDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            if ("".equals(value)) {
                return null;
            } else {
                try {
                    return TimeUtil.parse((String) value);
                } catch (ParseException e) {
                    throw new JsonParseException(e);
                }
            }
        }

        if (value instanceof Integer) {
            return new Date(((Integer) value)/* * 1000L*/);
        }

        if (value instanceof Long) {
            return new Date((Long) value);
        }

        throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + Date.class.getName());
    }

    /**
     * 转换时间元素
     * （包括毫秒时间戳）
     * @param value
     * @return
     */
    protected Timestamp getTimestamp(Object value) {
        if (value == null) {
            return null;
        }

        Date date = getDate(value);
        return new Timestamp(date.getTime());
    }

    /**
     * 转换数组元素
     * @param value
     * @return
     */
    protected Array getArray(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof List) {
            return new Array((List) value);
        }

        throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + Array.class.getName());
    }

    /**
     * 转换对象元素
     * @param value
     * @return
     */
    protected JSON getObject(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map) {
            return new JSON((Map) value);
        }

        throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + JSON.class.getName());
    }
}