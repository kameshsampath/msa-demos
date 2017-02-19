package org.workspace7.msa.calculator.client.util;

public class BaseException extends RuntimeException {

    public BaseException(String message) {
        super(message);
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public BaseException(String rc_202, String s) {
        super(rc_202+s);
    }
}
