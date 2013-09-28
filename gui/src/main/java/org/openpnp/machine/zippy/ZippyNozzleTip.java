package org.openpnp.machine.zippy;

import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.model.Location;
import org.simpleframework.xml.Attribute;

public class ZippyNozzleTip extends ReferenceNozzleTip {
	private Location offset; //X,Y at angle zero, Z=length offset from standard
	@Attribute(required = true) private float xoffset;
	@Attribute(required = true) private float yoffset;
	@Attribute(required = true) private float zoffset;

}
