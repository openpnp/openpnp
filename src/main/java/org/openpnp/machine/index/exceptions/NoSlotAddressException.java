package org.openpnp.machine.index.exceptions;

public class NoSlotAddressException extends Exception {
    public NoSlotAddressException() {
    }

    public NoSlotAddressException(String message) {
        super(message);
    }

    public NoSlotAddressException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSlotAddressException(Throwable cause) {
        super(cause);
    }

    public NoSlotAddressException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
