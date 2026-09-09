package com.yaonan.util.exception;

public class MyRuntimeException extends RuntimeException {
    public MyRuntimeException() { super(); }
    public MyRuntimeException(String message) { super(message); }
    public MyRuntimeException(String message, Throwable cause) { super(message, cause); }
    public MyRuntimeException(Throwable cause) { super(cause); }
}
