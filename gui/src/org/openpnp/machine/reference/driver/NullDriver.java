package org.openpnp.machine.reference.driver;

import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Part;
import org.simpleframework.xml.Attribute;

public class NullDriver implements ReferenceDriver {
	
	@Attribute(required=false)
	private String nullDriver;

	@Override
	public void home(ReferenceHead head, double feedRateMmPerMinute)
			throws Exception {
	}

	@Override
	public void moveTo(ReferenceHead head, double x, double y, double z,
			double c, double feedRateMmPerMinute) throws Exception {
		System.out.println(String.format("moveTo(%f, %f, %f, %f, %f)", x, y, z, c, feedRateMmPerMinute));
	}

	@Override
	public void pick(ReferenceHead head, Part part) throws Exception {
		System.out.println(String.format("pick(%s)", part.toString()));
	}

	@Override
	public void place(ReferenceHead head) throws Exception {
		System.out.println(String.format("place()"));
	}

	@Override
	public void actuate(ReferenceHead head, int index, boolean on)
			throws Exception {
		System.out.println(String.format("actuate(%d, %s)", index, on));
	}

	@Override
	public void setEnabled(boolean enabled) throws Exception {
	}

	@Override
	public void setReferenceMachine(ReferenceMachine machine) throws Exception {
	}
}
