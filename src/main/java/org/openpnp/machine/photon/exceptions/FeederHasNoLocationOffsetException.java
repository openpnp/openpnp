package org.openpnp.machine.photon.exceptions;

public class FeederHasNoLocationOffsetException extends Exception {
    public FeederHasNoLocationOffsetException() {
    }

    public FeederHasNoLocationOffsetException(String message) {
        super(message);
    }

    public FeederHasNoLocationOffsetException(String message, Throwable cause) {
        super(message, cause);
    }

    public FeederHasNoLocationOffsetException(Throwable cause) {
        super(cause);
    }

    public FeederHasNoLocationOffsetException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
