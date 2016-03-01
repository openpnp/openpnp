/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
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

package org.openpnp.gui.support;

import java.awt.Component;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageBoxes {
    private static final Logger logger = LoggerFactory.getLogger(MessageBoxes.class);

    public static void errorBox(Component parent, String title, Throwable cause) {
        String message = null;
        if (cause != null) {
            message = cause.getMessage();
            if (message == null || message.trim().isEmpty()) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter writer = new PrintWriter(stringWriter);
                cause.printStackTrace(writer);
                writer.close();
                message = stringWriter.toString();
            }
        }
        if (message == null) {
            message = "No message supplied.";
        }
        logger.debug("{}: {}", title, cause);
        message = message.replaceAll("\n", "<br/>");
        message = message.replaceAll("\r", "");
        message = "<html><body width=\"400\">" + message + "</body></html>";
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void errorBox(Component parent, String title, String message) {
        if (message == null) {
            message = "";
        }
        logger.debug("{}: {}", title, message);
        message = message.replaceAll("\n", "<br/>");
        message = message.replaceAll("\r", "");
        message = "<html><body width=\"400\">" + message + "</body></html>";
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void notYetImplemented(Component parent) {
        errorBox(parent, "Not Yet Implemented", "This function is not yet implemented.");
    }
}
