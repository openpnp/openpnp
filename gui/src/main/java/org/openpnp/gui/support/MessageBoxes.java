/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.support;

import java.awt.Component;

import javax.swing.JOptionPane;

import org.openpnp.util.LineBreaker;

public class MessageBoxes {
	public static void errorBox(Component parent, String title, Throwable cause) {
		String message = null;
		if (cause != null) {
			message = cause.getMessage();
		}
		if (message == null) {
			message = "";
		}
		JOptionPane.showMessageDialog(parent,
				LineBreaker.formatLines(message, 60), title,
				JOptionPane.ERROR_MESSAGE);
	}

	public static void errorBox(Component parent, String title, String message) {
		if (message == null) {
			message = "";
		}
		JOptionPane.showMessageDialog(parent,
				LineBreaker.formatLines(message, 60), title,
				JOptionPane.ERROR_MESSAGE);
	}
}
