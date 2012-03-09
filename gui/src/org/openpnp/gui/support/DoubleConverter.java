package org.openpnp.gui.support;

import org.jdesktop.beansbinding.Converter;

public class DoubleConverter extends Converter<Double, String> {
	private String format;
	
	public DoubleConverter(String format) {
		this.format = format;
	}
	
	@Override
	public String convertForward(Double arg0) {
		return String.format(format, arg0);
	}

	@Override
	public Double convertReverse(String arg0) {
		return Double.parseDouble(arg0);
	}

}
