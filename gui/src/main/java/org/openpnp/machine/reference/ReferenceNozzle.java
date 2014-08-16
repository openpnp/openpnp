package org.openpnp.machine.reference;

import java.util.ArrayList;

import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractNozzle;
import org.openpnp.spi.base.SimplePropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceNozzle extends AbstractNozzle implements
        ReferenceHeadMountable {
    private final static Logger logger = LoggerFactory
            .getLogger(ReferenceNozzle.class);

    @Element
    private Location headOffsets;

    @Attribute(required = false)
    private int pickDwellMilliseconds;

    @Attribute(required = false)
    private int placeDwellMilliseconds;
    
    @Attribute(required = false)
    private String currentNozzleTipId;
    
    @Attribute(required = false)
    private boolean changerEnabled = false;
    
    protected NozzleTip nozzleTip;

    protected ReferenceMachine machine;
    protected ReferenceDriver driver;

    public ReferenceNozzle() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration)
                    throws Exception {
                machine = (ReferenceMachine) configuration.getMachine();
                driver = machine.getDriver();
                nozzleTip = nozzleTips.get(currentNozzleTipId);
            }
        });
    }

    public int getPickDwellMilliseconds() {
        return pickDwellMilliseconds;
    }

    public void setPickDwellMilliseconds(int pickDwellMilliseconds) {
        this.pickDwellMilliseconds = pickDwellMilliseconds;
    }

    public int getPlaceDwellMilliseconds() {
        return placeDwellMilliseconds;
    }

    public void setPlaceDwellMilliseconds(int placeDwellMilliseconds) {
        this.placeDwellMilliseconds = placeDwellMilliseconds;
    }

    @Override
    public Location getHeadOffsets() {
        return headOffsets;
    }
    
    @Override
    public void setHeadOffsets(Location headOffsets) {
        this.headOffsets = headOffsets;
    }

    @Override
    public NozzleTip getNozzleTip() {
        return nozzleTip;
    }

    @Override
    public boolean canPickAndPlace(Feeder feeder, Location placeLocation) {
        boolean result = true;
		logger.debug("{}.canPickAndPlace({},{}) => {}", new Object[]{getId(), feeder, placeLocation, result});
    	return result;
	}

    @Override
    public void pick() throws Exception {
		logger.debug("{}.pick()", getId());
		if (nozzleTip == null) {
		    throw new Exception("Can't pick, no nozzle tip loaded");
		}
		driver.pick(this);
        machine.fireMachineHeadActivity(head);
        Thread.sleep(pickDwellMilliseconds);
    }

    @Override
    public void place() throws Exception {
		logger.debug("{}.place()", getId());
        if (nozzleTip == null) {
            throw new Exception("Can't place, no nozzle tip loaded");
        }
		driver.place(this);
        machine.fireMachineHeadActivity(head);
        Thread.sleep(placeDwellMilliseconds);
    }

    @Override
    public void moveTo(Location location, double speed) throws Exception {
        logger.debug("{}.moveTo({}, {})", new Object[] { id, location, speed } );
        driver.moveTo(this, location, speed);
        machine.fireMachineHeadActivity(head);
    }

    @Override
    public void moveToSafeZ(double speed) throws Exception {
		logger.debug("{}.moveToSafeZ({})", new Object[]{getId(), speed});
        Location l = new Location(getLocation().getUnits(), Double.NaN,
                Double.NaN, 0, Double.NaN);
        driver.moveTo(this, l, speed);
        machine.fireMachineHeadActivity(head);
    }
    
    @Override
    public void loadNozzleTip(NozzleTip nozzleTip) throws Exception {
        if (this.nozzleTip == nozzleTip) {
            return;
        }
        if (!changerEnabled) {
            throw new Exception("Can't load nozzle tip, nozzle tip changer is not enabled.");
        }
        unloadNozzleTip();
        logger.debug("{}.loadNozzleTip({}): Start", new Object[]{getId(), nozzleTip.getId()});
        ReferenceNozzleTip nt = (ReferenceNozzleTip) nozzleTip;
        logger.debug("{}.loadNozzleTip({}): moveToSafeZ", new Object[]{getId(), nozzleTip.getId()});
        moveToSafeZ(1.0);
        logger.debug("{}.loadNozzleTip({}): moveTo Start Location", new Object[]{getId(), nozzleTip.getId()});
        moveTo(nt.getChangerStartLocation(), 1.0);
        logger.debug("{}.loadNozzleTip({}): moveTo Mid Location", new Object[]{getId(), nozzleTip.getId()});
        moveTo(nt.getChangerMidLocation(), 0.25);
        logger.debug("{}.loadNozzleTip({}): moveTo End Location", new Object[]{getId(), nozzleTip.getId()});
        moveTo(nt.getChangerEndLocation(), 1.0);
        moveToSafeZ(1.0);
        logger.debug("{}.loadNozzleTip({}): Finished", new Object[]{getId(), nozzleTip.getId()});
        this.nozzleTip = nozzleTip;
        currentNozzleTipId = nozzleTip.getId();
        Configuration.get().setDirty(true);
    }

    @Override
    public void unloadNozzleTip() throws Exception {
        if (nozzleTip == null) {
            return;
        }
        if (!changerEnabled) {
            throw new Exception("Can't unload nozzle tip, nozzle tip changer is not enabled.");
        }
        logger.debug("{}.unloadNozzleTip(): Start", new Object[]{getId()});
        ReferenceNozzleTip nt = (ReferenceNozzleTip) nozzleTip;
        logger.debug("{}.unloadNozzleTip(): moveToSafeZ", new Object[]{getId()});
        moveToSafeZ(1.0);
        logger.debug("{}.unloadNozzleTip(): moveTo End Location", new Object[]{getId()});
        moveTo(nt.getChangerEndLocation(), 1.0);
        logger.debug("{}.unloadNozzleTip(): moveTo Mid Location", new Object[]{getId()});
        moveTo(nt.getChangerMidLocation(), 1.0);
        logger.debug("{}.unloadNozzleTip(): moveTo Start Location", new Object[]{getId()});
        moveTo(nt.getChangerStartLocation(), 0.25);
        moveToSafeZ(1.0);
        logger.debug("{}.unloadNozzleTip(): Finished", new Object[]{getId()});
        nozzleTip = null;
        currentNozzleTipId = null;
        Configuration.get().setDirty(true);
    }

    @Override
    public Location getLocation() {
        return driver.getLocation(this);
    }
    
    public boolean isChangerEnabled() {
        return changerEnabled;
    }

    public void setChangerEnabled(boolean changerEnabled) {
        this.changerEnabled = changerEnabled;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceNozzleConfigurationWizard(this);
    }
    
	@Override
    public String getPropertySheetHolderTitle() {
	    return getClass().getSimpleName() + " " + getId();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        ArrayList<PropertySheetHolder> children = new ArrayList<PropertySheetHolder>();
        children.add(new SimplePropertySheetHolder("Nozzles", getNozzleTips()));
        return children.toArray(new PropertySheetHolder[]{});
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard())
        };
    }
        
    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
	public String toString() {
		return getId();
	}
}
