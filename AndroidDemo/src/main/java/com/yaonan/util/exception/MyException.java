package com.yaonan.util.exception;

/**
 * 自定义受检异常：用于业务代码抛出需要显式捕获或声明的异常。
 */
public class MyException extends Exception {
    /** 无参构造。 */
    public MyException() { super(); }
    /** 仅携带异常信息。 */
    public MyException(String message) { super(message); }
    /** 携带异常信息与原因。 */
    public MyException(String message, Throwable cause) { super(message, cause); }
    /** 仅携带原因。 */
    public MyException(Throwable cause) { super(cause); }
}
