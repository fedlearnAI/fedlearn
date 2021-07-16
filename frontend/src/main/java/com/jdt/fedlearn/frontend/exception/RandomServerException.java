package com.jdt.fedlearn.frontend.exception;

public class RandomServerException extends RuntimeException{


    private static final long serialVersionUID = -4921547405344670799L;

    public RandomServerException() {
        super();
    }

    public RandomServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RandomServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public RandomServerException(String message) {
        super(message);
    }

    public RandomServerException(Throwable cause) {
        super(cause);
    }

}
