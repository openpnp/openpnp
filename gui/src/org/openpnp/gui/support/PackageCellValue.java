package org.openpnp.gui.support;

import org.openpnp.model.Configuration;

public class PackageCellValue {
	private static Configuration configuration;
	private org.openpnp.model.Package packag;
	
	public static void setConfiguration(Configuration configuration) {
		PackageCellValue.configuration = configuration;
	}
	
	public PackageCellValue(org.openpnp.model.Package packag) {
		this.packag = packag;
	}
	
	public PackageCellValue(String value) {
		org.openpnp.model.Package packag = configuration.getPackage(value);
		if (packag == null) {
			throw new NullPointerException();
		}
		this.packag = packag;
	}

	public org.openpnp.model.Package getPackage() {
		return packag;
	}

	public void setPackage(org.openpnp.model.Package packag) {
		this.packag = packag;
	}
	
	@Override
	public String toString() {
		return packag == null ? "" : packag.getId();
	}
}
