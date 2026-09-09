package com.yaonan.util.http;

import static com.yaonan.util.global.Global.TAG;

import android.util.Log;

import com.yaonan.util.exception.ExceptionUtil;
import com.yaonan.util.io.FileUtil;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * 接口工具类
 */
public class API {
    /**
     * 是否为成功的HTTP状态码
     * @param statusCode
     * @return
     */
    public static boolean isSuccess(int statusCode) {
        return (statusCode >= 200 && statusCode < 300 || statusCode == 304);
    }

    /**
     * 是否为失败的HTTP状态码
     * @param statusCode
     * @return
     */
    public static boolean isError(int statusCode) {
        return !isSuccess(statusCode);
    }

    /* ********************************************** 自定义httpclient ************************************************/

    /**
     * @see #sendOriginal
     * @see #getCertificates
     */
    static {
        HttpsURLConnection.setDefaultSSLSocketFactory(getSSLSocketFactory());
    }

    /**
     * 屏蔽ssl校验
     * @return
     */
    public static SSLSocketFactory getSSLSocketFactory() {
        return HTTPUtil.getSSLContext().getSocketFactory();
    }

    /**
     * 原生发送请求
     * 不能在安卓UI线程中执行！
     * 现在的安卓网络权限已不用授权！
     * 可以判断当前网络是否可用：
         ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
         NetworkInfo networkInfo = cm.getActiveNetworkInfo();
         if (networkInfo != null && networkInfo.isConnected()) {

         } else {
            Log.e(TAG, "网络未连接");
         }
     * @param isPost
     * @param url
     * @param headers ?为String或List<String>
     * @param bytes
     * @return
     * @throws java.io.InterruptedIOException 按键精灵中断操作要注意
     */
    public static Response sendOriginal(boolean isPost, String url,
                                        Map<String, ?> headers, byte[] bytes) {
        int _connectTimeout = 21_000; // 连接超时
        int _socketTimeout = 120_000; // 读取超时
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();

            // 超时等配置
            conn.setConnectTimeout(_connectTimeout);
            conn.setReadTimeout(_socketTimeout);

            // 请求头
            if (headers != null) {
                List<KeyValue<String, String>> keyValues = HTTPUtil.mapToKeyValueList(headers);
                for (KeyValue<String, String> keyValue : keyValues) {
                    conn.addRequestProperty(keyValue.key, keyValue.value);
                }
            }
            //conn.setUseCaches(false); // Cache-Control: no-cache
            conn.setRequestProperty("Accept-Encoding", "gzip"); // set覆盖
            conn.setRequestProperty("Connection", "close");

            // 发送请求
            if (isPost) {
                conn.setRequestMethod("POST");
                // 请求体
                if (bytes != null) {
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(bytes);
                        os.flush();
                    }
                }
            }/* else {
                conn.setRequestMethod("GET");
            }*/

            // 发送请求
            conn.connect();

            // 状态行
            int statusCode = conn.getResponseCode();
            String statusLine = "";

            // 响应头
            Map<String, List<String>> headerFields = conn.getHeaderFields();
            Map<String, List<String>> responseHeaders = new HashMap<>();
            for (Map.Entry<String, List<String>> header : headerFields.entrySet()) {
                String key = header.getKey();
                List<String> val = header.getValue();

                if (key != null) {
                    responseHeaders.put(key, val);
                } else if (val.size() > 0) {
                    statusLine = val.get(0);
                }
            }

            // 响应体，直接放入了Response，如果是大文件流需要单独处理
            String encoding = conn.getContentEncoding();
            InputStream is = conn.getErrorStream() == null ? conn.getInputStream() : conn.getErrorStream();
            if ("gzip".equalsIgnoreCase(encoding)) {
                is = new GZIPInputStream(is);
            }
            try (BufferedInputStream bis = new BufferedInputStream(is)) {
                byte[] body = FileUtil.toByteArray(bis);
                return new Response(statusCode, statusLine, body, responseHeaders);
            }
        } catch (Exception e) {
            Log.e(TAG, "监控已处理的异常，接口调用");
            ExceptionUtil.getStackTrace(e);
            return new Response(0, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}