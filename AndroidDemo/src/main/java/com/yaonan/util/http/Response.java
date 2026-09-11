package com.yaonan.util.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yaonan.util.codec.CharsetUtil;
import com.yaonan.util.exception.ExceptionUtil;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 接口接收到的响应对象
 */
public class Response {
    /**
     * 状态码
     */
    private int statusCode;

    /**
     * 状态行
     */
    private String statusLine;

    /**
     * 响应体
     */
    private byte[] body;

    /**
     * 响应头
     */
    private Map<String, List<String>> headers;

    /**
     * 调用异常信息
     */
    @JsonIgnore
    private Exception e;

    /**
     * 构造正常响应对应的响应对象。
     *
     * @param statusCode 状态码
     * @param statusLine 状态行
     * @param body       响应体
     * @param headers    响应头
     */
    public Response(int statusCode, String statusLine, byte[] body, Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.statusLine = statusLine;
        this.body = body;
        this.headers = headers;
    }

    /**
     * 构造请求异常时对应的响应对象。
     *
     * @param statusCode 状态码（异常时为 0）
     * @param e          请求异常
     */
    public Response(int statusCode, Exception e) {
        this.statusCode = statusCode;
        this.e = e;
    }

    /**
     * 状态码是否成功
     * @return
     */
    public boolean isSuccess() {
        return API.isSuccess(statusCode);
    }

    /**
     * 状态码是否失败
     * @return
     */
    public boolean isError() {
        return API.isError(statusCode);
    }

    /**
     * 获取请求异常信息
     * 转json可能会报错
     * @return
     */
    @JsonIgnore
    public Exception getError() {
        return e;
    }

    /**
     * 获取请求异常信息堆栈
     * @return
     */
    public String getStackTrace() {
        return ExceptionUtil.getStackTrace(e);
    }

    /**
     * 获取响应头，多个值只返回第一个
     * @param name
     * @return
     */
    public String getHeader(String name) {
        List<String> value = getHeaders(name);
        if (value.size() == 0) {
            return "";
        }
        return Objects.requireNonNullElse(value.get(0), "");
    }

    /**
     * 获取响应头，返回list形式
     * @param name
     * @return
     */
    public List<String> getHeaders(String name) {
        if (headers != null && name != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (name.equalsIgnoreCase(entry.getKey())) {
                    return Objects.requireNonNullElse(entry.getValue(), new ArrayList<>());
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * 获取全部响应头。
     *
     * @return 响应头集合
     */
    public Map<String, List<String>> getAllHeaders() {
        return headers;
    }

    /**
     * 获取Content-Type
     * @return
     */
    public String getContentType() {
        return getHeader("Content-Type");
    }

    /**
     * 获取状态码。
     *
     * @return 状态码
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * 获取状态行。
     *
     * @return 状态行
     */
    public String getStatusLine() {
        return statusLine;
    }

    /**
     * 以默认 UTF-8 编码获取响应体字符串。
     *
     * @return 响应体字符串
     */
    public String getBody() {
        return getBody(CharsetUtil.UTF_8);
    }

    /**
     * 以指定字符集获取响应体字符串。
     *
     * @param charset 字符集名称
     * @return 响应体字符串
     */
    public String getBody(String charset) {
        if (body == null) {
            return "";
        } else {
            return new String(body, Charset.forName(charset));
        }
    }

    /**
     * 获取响应体字节数组。
     *
     * @return 响应体字节数组
     */
    public byte[] getBodyBytes() {
        return body;
    }

    /**
     * 返回响应内容摘要：正常时拼接状态行、响应头和响应体，异常时返回空字符串。
     *
     * @return 响应摘要字符串
     */
    @Override
    public String toString() {
        if (e == null) {
            return statusLine + "\n" + headers + "\n" + getBody();
        }
        return "";
    }
}
