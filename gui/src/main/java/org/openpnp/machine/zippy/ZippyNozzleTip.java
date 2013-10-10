package org.openpnp.machine.zippy;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.feeder.wizards.ReferenceTapeFeederConfigurationWizard;
import org.openpnp.machine.reference.wizards.ReferenceActuatorConfigurationWizard;
import org.openpnp.model.Location;
import org.openpnp.spi.Head;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZippyNozzleTip extends ReferenceNozzleTip {
    private final static Logger logger = LoggerFactory
            .getLogger(ZippyNozzleTip.class);

    private ReferenceMachine machine;
    private ReferenceDriver driver;

    @Attribute(required = false)
    private int index;
    
    @Element(required = false)
    private Location nozzleOffsets;

    @Override
	public Wizard getConfigurationWizard() {
		return new ZippyNozzleTipConfigurationWizard(this);
	}
    public void setOffsets(Location nozzleOffsets) {
        this.nozzleOffsets = nozzleOffsets;
    }
    
    public Location getnozzleOffsets() {
        return nozzleOffsets;
    }

	public String getId() {
		return id;
	}

    public void moveTo(Location location, double speed) throws Exception {
		logger.debug("{}.moveTo({}, {})", new Object[] { getId(), location, speed } );
		driver.moveTo((ReferenceHeadMountable) this, location, speed);
        Head head = machine.getHead(getId()); //needs work
		machine.fireMachineHeadActivity(head);
    }
    public void setId(String id) {
        this.id = id;
    }

}
