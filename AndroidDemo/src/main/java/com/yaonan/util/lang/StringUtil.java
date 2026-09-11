package com.yaonan.util.lang;

import java.util.ArrayList;
import java.util.List;

/**
 * 字符串工具类：提供空判断与字符串拆分等常用操作。
 */
public class StringUtil {
    /**
     * 判断字符串是否为 null 或空串。
     *
     * @param str 待判断字符串
     * @return 是否为空
     */
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    /**
     * 判断字符串是否非空。
     *
     * @param str 待判断字符串
     * @return 是否非空
     */
    public static boolean isNotEmpty(CharSequence str) {
        return !isEmpty(str);
    }

    /**
     * 按分隔符拆分字符串。
     *
     * <p>separator 为 null 或空串时按空白字符拆分；否则按指定分隔符拆分。</p>
     *
     * @param str       待拆分字符串
     * @param separator 分隔符
     * @return 拆分后的字符串数组
     */
    public static String[] split(String str, String separator) {
        if (str == null) return null;
        int len = str.length();
        if (len == 0) return new String[0];

        if (separator == null || "".equals(separator)) {
            // 按空白字符拆分：连续空白会被视为分隔边界
            List<String> list = new ArrayList<>();
            int i = 0, start = 0;
            boolean match = false, lastMatch = false;
            while (i < len) {
                if (Character.isWhitespace(str.charAt(i))) {
                    lastMatch = true;
                    list.add(str.substring(start, i));
                    match = false;
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
            // 末尾若还有未截取的片段则补上
            if (match || lastMatch) list.add(str.substring(start, i));
            return list.toArray(new String[0]);
        } else {
            // 按指定分隔符拆分，保留空片段
            int separatorLength = separator.length();
            List<String> substrings = new ArrayList<>();
            int beg = 0, end = 0;
            while (end < len) {
                end = str.indexOf(separator, beg);
                if (end > -1) {
                    if (end > beg) {
                        substrings.add(str.substring(beg, end));
                        beg = end + separatorLength;
                    } else {
                        // 分隔符紧邻上一次分割点，产生空字符串片段
                        substrings.add("");
                        beg = end + separatorLength;
                    }
                } else {
                    // 找不到分隔符，剩余部分作为最后一个片段
                    substrings.add(str.substring(beg));
                    end = len;
                }
            }
            return substrings.toArray(new String[0]);
        }
    }

    /**
     * 按固定长度将字符串切分为若干子串。
     *
     * @param str  待切分字符串
     * @param size 每段长度（须大于 0）
     * @return 切分后的子串列表
     */
    public static List<String> split(String str, int size) {
        if (size <= 0) throw new IllegalArgumentException("size must be > 0");
        List<String> list = new ArrayList<>();
        int len = str.length();
        int next;
        for (int i = 0; i < len; i = next) {
            next = size + i;
            // 越界时取到字符串末尾
            String sub = (next < len) ? str.substring(i, next) : str.substring(i);
            list.add(sub);
        }
        return list;
    }
}
