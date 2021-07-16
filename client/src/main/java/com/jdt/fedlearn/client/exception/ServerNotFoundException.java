
package com.jdt.fedlearn.client.exception;

public class ServerNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 3816979698911773512L;

    public ServerNotFoundException() {
        super();
    }

    public ServerNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ServerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerNotFoundException(String message) {
        super(message);
    }

    public ServerNotFoundException(Throwable cause) {
        super(cause);
    }

}
