package org.openpnp.gui.support;

import org.jdesktop.beansbinding.Converter;
import org.openpnp.model.Length;

public class LengthConverter extends Converter<Length, String> {
	@Override
	public String convertForward(Length arg0) {
		return arg0.toString();
	}
	
	@Override
	public Length convertReverse(String arg0) {
		Length length = Length.parse(arg0, true);
		if (length == null) {
			throw new RuntimeException("Unable to parse " + arg0);
		}
		return length;
	}
}
