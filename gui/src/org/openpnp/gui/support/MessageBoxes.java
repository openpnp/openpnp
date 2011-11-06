package org.openpnp.gui.support;

import java.awt.Component;

import javax.swing.JOptionPane;

import org.openpnp.util.LineBreaker;

public class MessageBoxes {
	public static void errorBox(Component parent, String title, String message) {
		JOptionPane.showMessageDialog(parent, LineBreaker.formatLines(message, 60), title, 
				JOptionPane.ERROR_MESSAGE);
	}
}
