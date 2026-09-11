package com.yaonan.util.lang;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间工具类：提供日期时间字符串解析、格式化与常用时间格式常量。
 */
public class TimeUtil {

    /** 精确到秒的日期时间格式。 */
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    /** 精确到分钟的日期时间格式。 */
    public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
    /** 仅包含日期的格式。 */
    public static final String YYYY_MM_DD = "yyyy-MM-dd";

    /**
     * 解析时间字符串：带 "-" 按日期时间格式解析，否则按毫秒时间戳解析。
     *
     * @param time 时间字符串或毫秒时间戳字符串
     * @return 解析出的 Date
     * @throws ParseException 解析失败时抛出
     */
    public static Date parse(String time) throws ParseException {
        // 含 "-" 视为日期字符串，含 ":" 精确到秒，否则仅日期
        if (time.contains("-")) {
            if (time.contains(":")) {
                return new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS).parse(time);
            } else {
                return new SimpleDateFormat(YYYY_MM_DD).parse(time);
            }
        } else {
            // 纯数字视为毫秒时间戳
            return new Date(Long.parseLong(time));
        }
    }

    /**
     * 按指定格式解析时间字符串。
     *
     * @param time    时间字符串
     * @param pattern 日期格式
     * @return 解析出的 Date
     * @throws ParseException 解析失败时抛出
     */
    public static Date parse(String time, String pattern) throws ParseException {
        return new SimpleDateFormat(pattern).parse(time);
    }

    /**
     * 按指定格式格式化当前时间。
     *
     * @param pattern 日期格式
     * @return 格式化后的时间字符串
     */
    public static String now(String pattern) {
        return format(System.currentTimeMillis(), pattern);
    }

    /**
     * 将毫秒时间戳按指定格式格式化。
     *
     * @param time    毫秒时间戳
     * @param pattern 日期格式
     * @return 格式化后的时间字符串
     */
    public static String format(long time, String pattern) {
        return new SimpleDateFormat(pattern).format(time);
    }

    /**
     * 将 Date 按指定格式格式化。
     *
     * @param time    时间对象
     * @param pattern 日期格式
     * @return 格式化后的时间字符串
     */
    public static String format(Date time, String pattern) {
        return new SimpleDateFormat(pattern).format(time);
    }

    /** 获取当前时间字符串（精确到秒）。 */
    public static String nowTime() { return now(YYYY_MM_DD_HH_MM_SS); }
    /** 将毫秒时间戳格式化为时间字符串（精确到秒）。 */
    public static String formatTime(long time) { return format(time, YYYY_MM_DD_HH_MM_SS); }
    /** 将 Date 格式化为时间字符串（精确到秒）。 */
    public static String formatTime(Date time) { return format(time, YYYY_MM_DD_HH_MM_SS); }
    /** 获取当前日期字符串。 */
    public static String nowDate() { return now(YYYY_MM_DD); }
    /** 将毫秒时间戳格式化为日期字符串。 */
    public static String formatDate(long time) { return format(time, YYYY_MM_DD); }
    /** 将 Date 格式化为日期字符串。 */
    public static String formatDate(Date time) { return format(time, YYYY_MM_DD); }
}
