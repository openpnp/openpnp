package org.openpnp.logging;

public class LoggerFactory {
    private static Logger logger = new Logger();
    
    public static Logger getLogger(Class cls) {
        return logger;
    }
}
