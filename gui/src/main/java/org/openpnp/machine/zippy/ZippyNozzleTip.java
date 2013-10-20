package org.openpnp.machine.zippy;

import java.util.List;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.feeder.wizards.ReferenceTapeFeederConfigurationWizard;
import org.openpnp.machine.reference.wizards.ReferenceActuatorConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZippyNozzleTip extends ReferenceNozzleTip {

	private final static Logger logger = LoggerFactory
            .getLogger(ZippyNozzleTip.class);

    public ZippyNozzleTip(){
 //   	Location nozzleOffsets = new Location(); 
    }
    private ReferenceMachine machine;
    private ReferenceDriver driver;

    @Attribute(required = false)
    private int index;
    
    @Element(required = false)
    private Location nozzleOffsets;

	@Element(required = false)
	private Location mirrorStartLocation = new Location(LengthUnit.Millimeters);
	@Element(required = false)
	private Location mirrorMidLocation = new Location(LengthUnit.Millimeters);
	@Element(required = false)
	private Location mirrorEndLocation = new Location(LengthUnit.Millimeters);
	@Element(required = false)
	private Location changerStartLocation = new Location(LengthUnit.Millimeters);
	@Element(required = false)
	private Location changerMidLocation = new Location(LengthUnit.Millimeters);
	@Element(required = false)
	private Location changerEndLocation = new Location(LengthUnit.Millimeters);
    
    @Override
	public Wizard getConfigurationWizard() {
		return new ZippyNozzleTipConfigurationWizard(this);
	}
    public void setNozzleOffsets(Location nozzleOffsets) {
        this.nozzleOffsets = nozzleOffsets;
    }
    public Location getNozzleOffsets() {
        return nozzleOffsets;
    }
    //
    public void setmirrorStartLocation(Location mirrorStartLocation) {
        this.mirrorStartLocation = mirrorStartLocation;
    }
    
    public Location getmirrorStartLocation() {
        return mirrorStartLocation;
    }
    public void setmirrorMidLocation(Location MirrorMidLocation) {
        this.mirrorMidLocation = MirrorMidLocation;
    }
    
    public Location getmirrorMidLocation() {
        return mirrorMidLocation;
    }

    public void setmirrorEndLocation(Location mirrorEndLocation) {
        this.mirrorEndLocation = mirrorEndLocation;
    }
    
    public Location getmirrorEndLocation() {
        return mirrorEndLocation;
    }

    public void setchangerStartLocation(Location changerStartLocation) {
        this.changerStartLocation = changerStartLocation;
    }
    
    public Location getchangerStartLocation() {
        return changerStartLocation;
    }

    public void setchangerMidLocation(Location changerMidLocation) {
        this.changerMidLocation = changerMidLocation;
    }
    
    public Location getchangerMidLocation() {
        return changerMidLocation;
    }
    
    public void setchangerEndLocation(Location changerEndLocation) {
        this.changerEndLocation = changerEndLocation;
    }
    
    public Location getchangerEndLocation() {
        return changerEndLocation;
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
	public void load(Nozzle nozzle) throws Exception {
		//move to safe height
		nozzle.moveToSafeZ(1.0);
		//create local variables for movement
		Location changerStartLocation = this.changerStartLocation;
		Location changerMidLocation = this.changerMidLocation;
		Location changerEndLocation = this.changerEndLocation;
		
		//perform load operation
		nozzle.moveTo(changerStartLocation, 1.0);
		nozzle.moveTo(changerMidLocation, 1.0);
		nozzle.moveTo(changerEndLocation, 1.0);

		//move to safe height
		nozzle.moveToSafeZ(1.0);
		
	}
	public void unload(Nozzle nozzle) throws Exception {
		//move to safe height
		nozzle.moveToSafeZ(1.0);
		
		//create local variables for movement
		Location changerStartLocation = this.changerStartLocation;
		Location changerMidLocation = this.changerMidLocation;
		Location changerEndLocation = this.changerEndLocation;
		
		//perform unload operation
		nozzle.moveTo(changerEndLocation, 1.0);
		nozzle.moveTo(changerMidLocation, 1.0);
		nozzle.moveTo(changerStartLocation, 1.0);

		//move to safe height
		nozzle.moveToSafeZ(1.0);
	}
	public Location calibrate(Nozzle nozzle) throws Exception {
		//move to safe height
		nozzle.moveToSafeZ(1.0);
		
		//create local variables 
		Location newNozzleOffsets = this.nozzleOffsets;
		
		Location mirrorStartLocation = this.mirrorStartLocation;
		Location mirrorMidLocation = this.mirrorMidLocation;
		Location mirrorEndLocation = this.mirrorEndLocation;
		
		//move to mirror position
		nozzle.moveTo(mirrorStartLocation, 1.0);
		nozzle.moveTo(mirrorMidLocation, 1.0);
		nozzle.moveTo(mirrorEndLocation, 1.0);
		
		//do camera magic
		
		//move away from mirror position
		nozzle.moveTo(mirrorEndLocation, 1.0);
		nozzle.moveTo(mirrorMidLocation, 1.0);
		nozzle.moveTo(mirrorStartLocation, 1.0);
		

		return newNozzleOffsets;
	}
}
