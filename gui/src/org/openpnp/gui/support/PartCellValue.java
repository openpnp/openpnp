package org.openpnp.gui.support;

import org.openpnp.model.Configuration;
import org.openpnp.model.Part;

public class PartCellValue {
	private static Configuration configuration;
	private Part part;
	
	public static void setConfiguration(Configuration configuration) {
		PartCellValue.configuration = configuration;
	}
	
	public PartCellValue(Part part) {
		this.part = part;
	}
	
	public PartCellValue(String value) {
		Part part = configuration.getPart(value);
		if (part == null) {
			throw new NullPointerException();
		}
		this.part = part;
	}

	public Part getPart() {
		return part;
	}

	public void setPart(Part part) {
		this.part = part;
	}
	
	@Override
	public String toString() {
		return part == null ? "" : part.getId();
	}
}
