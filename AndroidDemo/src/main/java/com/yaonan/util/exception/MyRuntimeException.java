package com.yaonan.util.exception;

/**
 * 自定义运行时异常：用于业务代码抛出无需显式声明的异常。
 */
public class MyRuntimeException extends RuntimeException {
    /** 无参构造。 */
    public MyRuntimeException() { super(); }
    /** 仅携带异常信息。 */
    public MyRuntimeException(String message) { super(message); }
    /** 携带异常信息与原因。 */
    public MyRuntimeException(String message, Throwable cause) { super(message, cause); }
    /** 仅携带原因。 */
    public MyRuntimeException(Throwable cause) { super(cause); }
}
