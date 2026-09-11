package com.yaonan.util.exception;

import static com.yaonan.util.global.Global.TAG;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 异常工具类：提供异常堆栈信息的提取与日志记录。
 */
public class ExceptionUtil {
    /**
     * 获取异常的完整堆栈字符串，并输出错误日志。
     *
     * @param throwable 异常对象
     * @return 堆栈信息字符串
     */
    public static String getStackTrace(Throwable throwable) {
        // 将异常堆栈打印到字符串中
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.close();
        String result = sw.toString();
        Log.e(TAG, result);
        return result;
    }
}
