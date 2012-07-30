package org.openpnp.gui.support;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import org.openpnp.model.Identifiable;

@SuppressWarnings("serial")
public class IdentifiableListCellRenderer<T extends Identifiable> extends DefaultListCellRenderer {
	IdentifiableObjectToStringConverter<T> converter = new IdentifiableObjectToStringConverter<T>();
	
	@Override
	public Component getListCellRendererComponent(JList arg0, Object arg1,
			int arg2, boolean arg3, boolean arg4) {
		Component component = super.getListCellRendererComponent(arg0, arg1, arg2, arg3, arg4);
		((JLabel) component).setText(converter.getPreferredStringForItem(arg1));
		return component;
	}

}
