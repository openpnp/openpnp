package org.openpnp.gui.support;

import javax.swing.JTextField;

import org.openpnp.model.Configuration;
import org.openpnp.model.Location;

public class Helpers {
	public static void copyLocationIntoTextFields(Location l, JTextField x, JTextField y, JTextField z) {
		copyLocationIntoTextFields(l, x, y, z, null);
	}
	
	public static void copyLocationIntoTextFields(Location l, JTextField x, JTextField y, JTextField z, JTextField rotation) {
		if (x != null) {
			x.setText(String.format(Configuration.get().getLengthDisplayFormat(), l.getLengthX().getValue()));
		}
		if (y != null) {
			y.setText(String.format(Configuration.get().getLengthDisplayFormat(), l.getLengthY().getValue()));
		}
		if (z != null) {
			z.setText(String.format(Configuration.get().getLengthDisplayFormat(), l.getLengthZ().getValue()));
		}
		if (rotation != null) {
			rotation.setText(String.format(Configuration.get().getLengthDisplayFormat(), l.getRotation()));
		}
	}
}
