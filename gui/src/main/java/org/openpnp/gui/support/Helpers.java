package org.openpnp.gui.support;

import java.util.Collection;
import java.util.HashSet;

import javax.swing.JTable;
import javax.swing.JTextField;

import org.jdesktop.beansbinding.BeanProperty;
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
	
	/**
	 * Select the last row in a table. Handy for selecting a row that was
	 * just added.
	 * @param table
	 */
	public static void selectLastTableRow(JTable table) {
		table.clearSelection();
		int index = table.getRowCount() - 1;
		index = table.convertRowIndexToView(index);
		table.addRowSelectionInterval(index, index);
	}
	
	/**
	 * Create a unique name consisting of the prefix and an integer. The name
	 * is guaranteed to be unique within the properties of the given Collection
	 * using the given propertyName.
	 * @param prefix
	 * @param existingObjects Objects against which to compare the property
	 * identified by propertyName against for the unique name.
	 * @param propertyName The name of a String property.
	 */
	public static String createUniqueName(String prefix, Collection existingObjects, String propertyName) {
		HashSet<String> names = new HashSet<String>();
		BeanProperty<Object, String> property = BeanProperty.create(propertyName);
		for (Object o : existingObjects) {
			if (o != null) {
				names.add(property.getValue(o));
			}
		}
		for (int i = 1; i < Integer.MAX_VALUE; i++) {
			String name = prefix + i;
			if (!names.contains(name)) {
				return name;
			}
		}
		
		return null;
	}
}
