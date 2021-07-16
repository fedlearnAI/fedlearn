package com.jdt.fedlearn.coordinator.exception.jdchain;

public class StartTrainException extends RuntimeException{


    private static final long serialVersionUID = -6751554656273696393L;

    public StartTrainException() {
        super();
    }

    public StartTrainException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public StartTrainException(String message, Throwable cause) {
        super(message, cause);
    }

    public StartTrainException(String message) {
        super(message);
    }

    public StartTrainException(Throwable cause) {
        super(cause);
    }

}
