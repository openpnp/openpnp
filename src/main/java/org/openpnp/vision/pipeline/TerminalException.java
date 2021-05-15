package org.openpnp.vision.pipeline;

public class TerminalException extends Exception{
    private final Exception originalException;

    public TerminalException(Exception originalException) {
        super(originalException.getMessage(), originalException);
        this.originalException = originalException;
    }

    public Exception getOriginalException() {
        return originalException;
    }
}
