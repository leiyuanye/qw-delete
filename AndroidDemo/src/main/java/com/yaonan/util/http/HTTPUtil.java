package com.yaonan.util.http;

import com.yaonan.util.lang.StringUtil;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * httpclient工具类
 * Created by yaonan on 2021/3/11.
 */
public class HTTPUtil {
    /**
     * 值类型为String或List<String>的map，转List<KeyValue<String, String>>
     * @param map
     * @return
     */
    public static List<KeyValue<String, String>> mapToKeyValueList(Map<String, ?> map) {
        List<KeyValue<String, String>> list = new ArrayList<>();
        if (map == null) {
            return list;
        }

        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (StringUtil.isEmpty(key)) { // key为null或""跳过，val为null跳过
                continue;
            }

            if (val instanceof String) {
                list.add(new KeyValue<>(key, (String) val));
            } else if (val instanceof List) {
                for (Object value : (List) val) {
                    if (value instanceof String) {
                        list.add(new KeyValue<>(key, (String) value));
                    } else if (value != null) {
                        throw new IllegalArgumentException(
                                "Map value type must be a String or List<String>");
                    }
                }
            } else if (val != null) {
                throw new IllegalArgumentException(
                        "Map value type must be a String or List<String>");
            }
        }

        return list;
    }

    /**
     * 屏蔽ssl校验
     * @return
     */
    public static SSLContext getSSLContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                    }
                }
            }, null);
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException(e);
        }
    }
}
