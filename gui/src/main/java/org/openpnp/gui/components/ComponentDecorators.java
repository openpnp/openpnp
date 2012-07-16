package org.openpnp.gui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JTextField;

import org.openpnp.model.Configuration;
import org.openpnp.model.Length;

public class ComponentDecorators {
	/**
	 * Adds an auto selection decoration to the JTextField. Whenever the
	 * JTextField gains focus the text in it will be selected.
	 * @param textField
	 */
	public static void decorateWithAutoSelect(JTextField textField) {
		textField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent event) {
				((JTextField) event.getComponent()).selectAll();
			}
		});
		textField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				((JTextField) event.getSource()).selectAll();
			}
		});
	}
	
	/**
	 * Adds a length conversion decoration to the JTextField. When the
	 * JTextField loses focus or has it's action triggered the text
	 * will be converted to a Length value in the system units and then
	 * have it's text replaced with the value.
	 * @param textField
	 */
	public static void decorateWithLengthConversion(JTextField textField) {
		textField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				convertLength(((JTextField) event.getSource()));
			}
		});
		textField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent event) {
				convertLength(((JTextField) event.getSource()));
			}
		});
	}
	
	public static void decorateWithAutoSelectAndLengthConversion(JTextField textField) {
		decorateWithAutoSelect(textField);
		decorateWithLengthConversion(textField);
	}

	private static void convertLength(JTextField textField) {
		Length length = Length.parse(textField.getText(), false);
		if (length == null) {
			return;
		}
		if (length.getUnits() == null) {
			length.setUnits(Configuration.get().getSystemUnits());
		}
		length = length.convertToUnits(Configuration.get().getSystemUnits());
		textField.setText(String.format(Configuration.get().getLengthDisplayFormat(), length.getValue()));
	}
}
