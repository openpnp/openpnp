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

    abstract public void connect() throws Exception;
    abstract public void disconnect() throws Exception;

    abstract public String getConnectionName();

    abstract public String readLine() throws TimeoutException, IOException;
    abstract public void writeLine(String data) throws IOException;

    public void setLineEndingType(LineEndingType lineEndingType) {
        this.lineEndingType = lineEndingType;
    }
    
    public LineEndingType getLineEndingType() {
        return lineEndingType;
    }
}
