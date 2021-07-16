package com.jdt.fedlearn.core.exception;

public class WrongValueException extends RuntimeException {

    private static final long serialVersionUID = 3816979698933373512L;

    public WrongValueException() {
        super();
    }

    public WrongValueException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public WrongValueException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongValueException(String message) {
        super(message);
    }

    public WrongValueException(Throwable cause) {
        super(cause);
    }
}
