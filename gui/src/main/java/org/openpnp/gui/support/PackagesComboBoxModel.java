package org.openpnp.gui.support;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.DefaultComboBoxModel;

import org.openpnp.model.Configuration;

@SuppressWarnings("serial")
public class PackagesComboBoxModel extends DefaultComboBoxModel implements PropertyChangeListener {
	private IdentifiableComparator<org.openpnp.model.Package> comparator = new IdentifiableComparator<org.openpnp.model.Package>();

	public PackagesComboBoxModel() {
		addAllElements();
		Configuration.get().addPropertyChangeListener("packages", this);
	}
	
	private void addAllElements() {
		ArrayList<org.openpnp.model.Package> packages = new ArrayList<org.openpnp.model.Package>(Configuration.get().getPackages());
		Collections.sort(packages, comparator);
		for (org.openpnp.model.Package pkg : packages) {
			addElement(pkg);
		}
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		removeAllElements();
		addAllElements();
	}
}
