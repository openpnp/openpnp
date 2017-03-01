package org.openpnp.logging;

import java.io.OutputStream;
import java.io.PrintStream;

import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

/**
 * A SystemLogger class that redirects another stream like e.g. stdout or stderr to the tinylog logger
 */
public class SystemLogger extends PrintStream {

    private static final String lineSeparator = System.getProperty("line.separator");

    private Level logLevel;

    public SystemLogger(OutputStream out, Level logLevel) {
        super(out);
        this.logLevel = logLevel;
    }

    public SystemLogger(OutputStream out) {
        super(out);
        this.logLevel = Level.INFO;
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        super.write(buf, off, len);

        byte[] pb = new byte[len];
        System.arraycopy(buf, off, pb, 0, len);
        String str = new String(pb);
        str = str.replace(lineSeparator, "");

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
}
