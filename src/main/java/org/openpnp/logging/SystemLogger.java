package org.openpnp.logging;

import java.io.*;

import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

/**
 * A SystemLogger class that redirects another stream like e.g. stdout or stderr to the tinylog logger
 */
public class SystemLogger extends PrintStream {

    private StringBuilder logMessage = new StringBuilder();

    private Level logLevel;
    private OutputStream out;
    private boolean logToLogger = true;

    public SystemLogger(OutputStream out, Level logLevel) {
        super(out);
        this.out = out;
        this.logLevel = logLevel;

        // In order to stop logging on shutdown, we need to make sure that we do not call the Logger anymore
        Runtime.getRuntime().addShutdownHook(new Thread(() -> logToLogger = false));
    }

    private void flushLogMessage() {

        String str = logMessage.toString();
        logMessage.setLength(0); // set length of buffer to 0
        logMessage.trimToSize(); // trim the underlying buffer

        // There is no generic log function where one could pass the log level
        switch (logLevel) {
            case INFO:
                Logger.info(str);
                break;
            case ERROR:
                Logger.error(str);
                break;
        }

    }

    @Override
    public void write(byte[] buf, int off, int len) {
        /*
         * Log to tinylog as long as the Logger is available (before shutdown)
         * As tinylog will forward all logs to the console, we do not call the super method
         */
        if (logToLogger) {
            byte[] pb = new byte[len];
            System.arraycopy(buf, off, pb, 0, len);
            String str = new String(pb);

            // String builder cannot handle a byte array
            for (int i = 0; i < len; i++) {
                if (i == (len - 1)) {
                    // The log message will appen a line break, so we remove the original one here
                    if (pb[i] == '\n') {
                        logMessage.append(str, 0, str.length() - 1);
                        flushLogMessage();
                    } else {
                        logMessage.append(str, 0, str.length());
                    }
                }
            }
        } else {
            super.write(buf, off, len);
        }
    }
}
