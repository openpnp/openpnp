/*
 * Copyright (C) 2018 Paul Jones <paul@pauljones.id.au>
 *
 * This file is part of OpenPnP.
 *
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.driver;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jfree.chart.util.ArrayUtils;
import org.openpnp.model.Named;
import org.openpnp.util.Collect;
import org.openpnp.util.GcodeServer;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * Defines the interface for a simple communications IO driver, for example A serial port.
 *
 * This Driver interface is intended to model the minimum required functions to transfer
 * data from OpenPnP to a hardware controller.
 */
public abstract class ReferenceDriverCommunications {
    public enum LineEndingType {
        CR("\r"),
        LF("\n"),
        CRLF("\r\n");

        public final String lineEnding;

        private LineEndingType(String lineEnding) {
            this.lineEnding = lineEnding;
        }
        
        public String getLineEnding() {
            return lineEnding;
        }
    }
    
    @Attribute(required=false)
    protected LineEndingType lineEndingType = LineEndingType.LF;

    protected String driverName;

    abstract public void connect() throws Exception;
    abstract public void disconnect() throws Exception;

    /**
     * Set the driver name, so the connection can be easily associated with the driver.
     */
    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }
    abstract public String getConnectionName();

    abstract public void writeBytes(byte[] data) throws IOException;

    abstract public int read() throws TimeoutException, IOException;

    /**
     * Read a line from the input stream. Blocks for the default timeout. If the read times out a
     * TimeoutException is thrown. Any other failure to read results in an IOExeption;
     *
     * @return
     * @throws TimeoutException
     * @throws IOException
     */
    public String readLine() throws TimeoutException, IOException {
        return readUntil("\r\n");
    }

    public void writeLine(String data) throws IOException {
        byte [] line = Collect.concat(data.getBytes(), getLineEndingType().getLineEnding().getBytes());
        writeBytes(line);
    }

    /**
     * Read the input stream until one of the characters is found. Blocks for the default timeout. If the read times out
     * a TimeoutException is thrown. Any other failure to read results in an IOExeption;
     *
     * @param characters list of ending characters
     * @return
     * @throws TimeoutException
     * @throws IOException
     */
    protected String readUntil(String characters) throws TimeoutException, IOException {
        StringBuffer line = new StringBuffer();
        while (true) {
            int ch = -1;
            try {
                ch = read();
            }
            catch (TimeoutException e) {
                // In case an implementation has a read timeout, we must not stop reading.
                continue;
            }
            if (ch == -1) {
                return null;
            }
            else if (characters.indexOf((char)ch) >= 0) {
                if (line.length() > 0) {
                    return line.toString();
                }
            }
            else {
                line.append((char) ch);
            }
        }
    }

    public void write(int d) throws IOException {
        byte[] b = new byte[] { (byte) d };
        writeBytes(b);
    }

    public void setLineEndingType(LineEndingType lineEndingType) {
        this.lineEndingType = lineEndingType;
    }

    public LineEndingType getLineEndingType() {
        return lineEndingType;
    }

    public GcodeServer getGcodeServer() {
        return null;
    }
}
