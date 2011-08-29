package org.openpnp;


public class PackageDef {
	String reference;
	String name;
	Outline outline;
	
	public String getReference() {
		return reference;
	}
	
	public void setReference(String reference) {
		this.reference = reference;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public Outline getOutline() {
		return outline;
	}

	public void setOutline(Outline outline) {
		this.outline = outline;
	}
	
	@Override
	public String toString() {
		return String.format("ref %s, outline (%s)", reference, outline);
	}
}
