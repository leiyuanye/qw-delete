package com.yaonan.util.exception;

import static com.yaonan.util.global.Global.TAG;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {
    public static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.close();
        String result = sw.toString();
        Log.e(TAG, result);
        return result;
    }
}
