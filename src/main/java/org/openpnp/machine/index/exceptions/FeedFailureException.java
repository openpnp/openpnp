package org.openpnp.machine.index.exceptions;

public class FeedFailureException extends Exception {
    public FeedFailureException() {
    }

    public FeedFailureException(String message) {
        super(message);
    }

    public FeedFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public FeedFailureException(Throwable cause) {
        super(cause);
    }

    public FeedFailureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
