package org.openpnp.machine.photon.exceptions;

public class UnconfiguredSlotException extends Exception {
    public UnconfiguredSlotException() {
    }

    public UnconfiguredSlotException(String message) {
        super(message);
    }

    public UnconfiguredSlotException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnconfiguredSlotException(Throwable cause) {
        super(cause);
    }

    public UnconfiguredSlotException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
