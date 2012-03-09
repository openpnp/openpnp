package org.openpnp.gui.support;

import org.jdesktop.beansbinding.Converter;

public class IntegerConverter extends Converter<Integer, String> {
	private String format;
	
	public IntegerConverter(String format) {
		this.format = format;
	}
	
	@Override
	public String convertForward(Integer arg0) {
		return String.format(format, arg0);
	}

	@Override
	public Integer convertReverse(String arg0) {
		return Integer.parseInt(arg0);
	}

}
