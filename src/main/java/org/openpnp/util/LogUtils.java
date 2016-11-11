package org.openpnp.util;

import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

public class LogUtils {
    public static boolean isDebugEnabled() {
        return Logger.getLevel().compareTo(Level.DEBUG) <= 0;
    }
}
