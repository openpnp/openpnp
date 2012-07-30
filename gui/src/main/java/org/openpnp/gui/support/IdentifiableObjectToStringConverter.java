package org.openpnp.gui.support;

import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;
import org.openpnp.model.Identifiable;

public class IdentifiableObjectToStringConverter<T extends Identifiable> extends ObjectToStringConverter {
	public String getPreferredStringForItem(Object o) {
		if (o == null) {
			return null;
		}
		T t = (T) o;
		return t.getId() == null ? "" : t.getId();
	}
}
