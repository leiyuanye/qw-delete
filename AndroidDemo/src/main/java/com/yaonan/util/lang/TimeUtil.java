package com.yaonan.util.lang;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
    public static final String YYYY_MM_DD = "yyyy-MM-dd";

    public static Date parse(String time) throws ParseException {
        if (time.contains("-")) {
            if (time.contains(":")) {
                return new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS).parse(time);
            } else {
                return new SimpleDateFormat(YYYY_MM_DD).parse(time);
            }
        } else {
            return new Date(Long.parseLong(time));
        }
    }

    public static Date parse(String time, String pattern) throws ParseException {
        return new SimpleDateFormat(pattern).parse(time);
    }

    public static String now(String pattern) {
        return format(System.currentTimeMillis(), pattern);
    }

    public static String format(long time, String pattern) {
        return new SimpleDateFormat(pattern).format(time);
    }

    public static String format(Date time, String pattern) {
        return new SimpleDateFormat(pattern).format(time);
    }

    public static String nowTime() { return now(YYYY_MM_DD_HH_MM_SS); }
    public static String formatTime(long time) { return format(time, YYYY_MM_DD_HH_MM_SS); }
    public static String formatTime(Date time) { return format(time, YYYY_MM_DD_HH_MM_SS); }
    public static String nowDate() { return now(YYYY_MM_DD); }
    public static String formatDate(long time) { return format(time, YYYY_MM_DD); }
    public static String formatDate(Date time) { return format(time, YYYY_MM_DD); }
}
