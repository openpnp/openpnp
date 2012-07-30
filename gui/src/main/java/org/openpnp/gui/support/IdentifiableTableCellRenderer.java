package org.openpnp.gui.support;

import javax.swing.table.DefaultTableCellRenderer;

import org.openpnp.model.Identifiable;

@SuppressWarnings("serial")
public class IdentifiableTableCellRenderer<T extends Identifiable> extends DefaultTableCellRenderer {
	IdentifiableObjectToStringConverter<T> converter = new IdentifiableObjectToStringConverter<T>();
	
	@Override
	protected void setValue(Object value) {
		setText(converter.getPreferredStringForItem(value));
	}
}
