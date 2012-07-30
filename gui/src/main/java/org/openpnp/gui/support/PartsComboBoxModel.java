package org.openpnp.gui.support;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.DefaultComboBoxModel;

import org.openpnp.model.Configuration;
import org.openpnp.model.Part;

@SuppressWarnings("serial")
public class PartsComboBoxModel extends DefaultComboBoxModel implements PropertyChangeListener {
	private IdentifiableComparator<Part> comparator = new IdentifiableComparator<Part>();
	
	public PartsComboBoxModel() {
		addAllElements();
		Configuration.get().addPropertyChangeListener("parts", this);
	}
	
	private void addAllElements() {
		ArrayList<Part> parts = new ArrayList<Part>(Configuration.get().getParts());
		Collections.sort(parts, comparator);
		for (Part part : parts) {
			addElement(part);
		}
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		removeAllElements();
		addAllElements();
	}
}
