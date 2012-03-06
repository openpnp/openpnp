package org.openpnp.machine.reference;

import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class ReferenceActuator implements Actuator {
	@Attribute
	private String id;
	@Attribute
	private int index;
	@Element(required=false)
	private Location location;
	
	private ReferenceMachine machine;
	private ReferenceHead head;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}
	
	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public void actuate(boolean on) throws Exception {
		if (head != null) {
			head.getMachine().getDriver().actuate(head, index, on);
		}
		else {
			machine.getDriver().actuate(head, index, on);
		}
		// TODO Build out fireMachineActuatorActivity
//		machine.fireMachineHeadActivity(machine, this);
	}
	
	public void setReferenceHead(ReferenceHead head) {
		this.head = head;
	}
	
	public void setReferenceMachine(ReferenceMachine machine) {
		this.machine = machine;
	}
}
