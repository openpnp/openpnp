package org.openpnp.gui.support;

import javax.swing.JTextField;

import org.openpnp.model.Configuration;
import org.openpnp.model.Location;

public class Helpers {
	public static void copyLocationIntoTextFields(Location l, JTextField x, JTextField y, JTextField z) {
		x.setText(String.format(Configuration.get().getLengthDisplayFormat(), l.getLengthX().getValue()));
		y.setText(String.format(Configuration.get().getLengthDisplayFormat(), l.getLengthY().getValue()));
		z.setText(String.format(Configuration.get().getLengthDisplayFormat(), l.getLengthZ().getValue()));
	}
	
	public static void copyLocationIntoTextFields(Location l, JTextField x, JTextField y, JTextField z, JTextField rotation) {
		x.setText(String.format(Configuration.get().getLengthDisplayFormat(), l.getLengthX().getValue()));
		y.setText(String.format(Configuration.get().getLengthDisplayFormat(), l.getLengthY().getValue()));
		z.setText(String.format(Configuration.get().getLengthDisplayFormat(), l.getLengthZ().getValue()));
		rotation.setText(String.format(Configuration.get().getLengthDisplayFormat(), l.getRotation()));
	}
}
