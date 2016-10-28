package org.openpnp.logging;

import org.pmw.tinylog.Level;

public class Logger {
    public void error(String message, Object... args) {
        org.pmw.tinylog.Logger.info(message, args);
    }
    
    public void error(String message, Throwable t) {
        org.pmw.tinylog.Logger.info(message, t);
    }
    
    public void warn(String message, Object... args) {
        org.pmw.tinylog.Logger.info(message, args);
    }
    
    public void warn(String message, Throwable t) {
        org.pmw.tinylog.Logger.info(message, t);
    }
    
    public void info(String message, Object... args) {
        org.pmw.tinylog.Logger.info(message, args);
    }
    
    public void info(String message, Throwable t) {
        org.pmw.tinylog.Logger.info(message, t);
    }
    
    public void debug(String message, Object... args) {
        org.pmw.tinylog.Logger.info(message, args);
    }
    
    public void debug(String message, Throwable t) {
        org.pmw.tinylog.Logger.info(message, t);
    }
    
    public void trace(String message, Object... args) {
        org.pmw.tinylog.Logger.info(message, args);
    }
    
    public void trace(String message, Throwable t) {
        org.pmw.tinylog.Logger.info(message, t);
    }    
    
    public boolean isDebugEnabled() {
        return org.pmw.tinylog.Logger.getLevel() == Level.DEBUG ||
                org.pmw.tinylog.Logger.getLevel() == Level.TRACE;
    }
}
