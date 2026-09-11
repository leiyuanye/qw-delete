package com.yaonan.util.json;

/**
 * JSON 解析异常：用于包装 JSON 解析过程中产生的各类底层异常，统一为运行时异常向上抛出。
 */
public class JsonParseException extends RuntimeException {
    /**
     * 通过原始异常构造。
     *
     * @param cause 底层异常
     */
    public JsonParseException(Throwable cause) {
        super(cause);
    }
}