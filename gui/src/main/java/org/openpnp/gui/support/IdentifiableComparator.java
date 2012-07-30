package org.openpnp.gui.support;

import java.util.Comparator;

import org.openpnp.model.Identifiable;

public class IdentifiableComparator<T extends Identifiable> implements Comparator<T> {
	public int compare(T o1, T o2) {
		if (o1 == null) {
			return 1;
		}
		else if (o2 == null) {
			return -1;
		}
		else {
			return o1.getId().compareTo(o2.getId());
		}
	}
}
