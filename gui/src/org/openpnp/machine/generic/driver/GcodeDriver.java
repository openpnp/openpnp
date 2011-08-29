package org.openpnp.machine.generic.driver;

import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.LengthUnit;
import org.openpnp.Part;
import org.openpnp.machine.generic.GenericDriver;
import org.openpnp.machine.generic.GenericHead;
import org.openpnp.util.LengthUtil;
import org.w3c.dom.Node;

public class GcodeDriver implements GenericDriver {
	private double x, y, z, a;
	
	@Override
	public void configure(Node n) {
		
	}
	
	@Override
	public void prepareJob(Configuration configuration, Job job)
			throws Exception {
	}

	@Override
	public void actuate(GenericHead head, int index, boolean on) throws Exception {
	}

	@Override
	public void home(GenericHead head) throws Exception {
		moveTo(head, 0, 0, 0, 0);
	}

	@Override
	public void moveTo(GenericHead head, double x, double y, double z, double a) throws Exception {
		x = LengthUtil.convertLength(x, LengthUnit.Millimeters, LengthUnit.Inches);
		y = LengthUtil.convertLength(y, LengthUnit.Millimeters, LengthUnit.Inches);
		z = LengthUtil.convertLength(z, LengthUnit.Millimeters, LengthUnit.Inches);
		
		
		StringBuffer sb = new StringBuffer();
		if (x != this.x) {
			sb.append(String.format(" X%2.4f", x));
		}
		if (y != this.y) {
			sb.append(String.format(" Y%2.4f", y));
		}
		if (z != this.z) {
			sb.append(String.format(" Z%2.4f", z));
		}
		if (a != this.a) {
			sb.append(String.format(" A%2.4f", a));
		}
		if (sb.length() > 0) {
			System.out.println("G0 " + sb.toString());
		}
		this.x = x;
		this.y = y;
		this.z = z;
		this.a = a;
	}

	@Override
	public void pick(GenericHead head, Part part) throws Exception {
	}

	@Override
	public void place(GenericHead head) throws Exception {
	}
}
