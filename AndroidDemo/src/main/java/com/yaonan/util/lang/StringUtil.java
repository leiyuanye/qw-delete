package com.yaonan.util.lang;

import java.util.ArrayList;
import java.util.List;

public class StringUtil {
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(CharSequence str) {
        return !isEmpty(str);
    }

    public static String[] split(String str, String separator) {
        if (str == null) return null;
        int len = str.length();
        if (len == 0) return new String[0];

        if (separator == null || "".equals(separator)) {
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
            if (match || lastMatch) list.add(str.substring(start, i));
            return list.toArray(new String[0]);
        } else {
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
                        substrings.add("");
                        beg = end + separatorLength;
                    }
                } else {
                    substrings.add(str.substring(beg));
                    end = len;
                }
            }
            return substrings.toArray(new String[0]);
        }
    }

    public static List<String> split(String str, int size) {
        if (size <= 0) throw new IllegalArgumentException("size must be > 0");
        List<String> list = new ArrayList<>();
        int len = str.length();
        int next;
        for (int i = 0; i < len; i = next) {
            next = size + i;
            String sub = (next < len) ? str.substring(i, next) : str.substring(i);
            list.add(sub);
        }
        return list;
    }
}
