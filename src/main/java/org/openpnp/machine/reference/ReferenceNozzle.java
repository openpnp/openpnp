package org.openpnp.machine.reference;

import java.util.ArrayList;

import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
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
    
    @Element(required=false)
    protected Length safeZ = new Length(0, LengthUnit.Millimeters);
   
    
    /**
     * If limitRotation is enabled the nozzle will reverse directions when
     * commanded to rotate past 180 degrees. So, 190 degrees becomes -170
     * and -190 becomes 170.
     */
    @Attribute(required = false)
    private boolean limitRotation = true;
    
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
    
    public boolean isLimitRotation() {
        return limitRotation;
    }

    public void setLimitRotation(boolean limitRotation) {
        this.limitRotation = limitRotation;
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
    public void pick() throws Exception {
		logger.debug("{}.pick()", getName());
		if (nozzleTip == null) {
		    throw new Exception("Can't pick, no nozzle tip loaded");
		}
		driver.pick(this);
        machine.fireMachineHeadActivity(head);
        Thread.sleep(pickDwellMilliseconds);
    }

    @Override
    public void place() throws Exception {
		logger.debug("{}.place()", getName());
        if (nozzleTip == null) {
            throw new Exception("Can't place, no nozzle tip loaded");
        }
		driver.place(this);
        machine.fireMachineHeadActivity(head);
        Thread.sleep(placeDwellMilliseconds);
    }

    @Override
    public void moveTo(Location location, double speed) throws Exception {
        logger.debug("{}.moveTo({}, {})", new Object[] { getName(), location, speed } );
        if (limitRotation && !Double.isNaN(location.getRotation()) && Math.abs(location.getRotation()) > 180) {
            if (location.getRotation() < 0) {
                location = location.derive(null, null, null, location.getRotation() + 360);
            }
            else {
                location = location.derive(null, null, null, location.getRotation() - 360);
            }
        }
        driver.moveTo(this, location, speed);
        machine.fireMachineHeadActivity(head);
    }

    @Override
    public void moveToSafeZ(double speed) throws Exception {
        logger.debug("{}.moveToSafeZ({})", new Object[] { getName(), speed } );
        Length safeZ = this.safeZ.convertToUnits(getLocation().getUnits());
        Location l = new Location(getLocation().getUnits(), Double.NaN,
                Double.NaN, safeZ.getValue(), Double.NaN);
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
        logger.debug("{}.loadNozzleTip({}): Start", new Object[]{getName(), nozzleTip.getName()});
        ReferenceNozzleTip nt = (ReferenceNozzleTip) nozzleTip;
        logger.debug("{}.loadNozzleTip({}): moveToSafeZ", new Object[]{getName(), nozzleTip.getName()});
        moveToSafeZ(1.0);
        logger.debug("{}.loadNozzleTip({}): moveTo Start Location", new Object[]{getName(), nozzleTip.getName()});
        moveTo(nt.getChangerStartLocation(), 1.0);
        logger.debug("{}.loadNozzleTip({}): moveTo Mid Location", new Object[]{getName(), nozzleTip.getName()});
        moveTo(nt.getChangerMidLocation(), 0.25);
        logger.debug("{}.loadNozzleTip({}): moveTo End Location", new Object[]{getName(), nozzleTip.getName()});
        moveTo(nt.getChangerEndLocation(), 1.0);
        moveToSafeZ(1.0);
        logger.debug("{}.loadNozzleTip({}): Finished", new Object[]{getName(), nozzleTip.getName()});
        this.nozzleTip = nozzleTip;
        currentNozzleTipId = nozzleTip.getId();
    }

    @Override
    public void unloadNozzleTip() throws Exception {
        if (nozzleTip == null) {
            return;
        }
        if (!changerEnabled) {
            throw new Exception("Can't unload nozzle tip, nozzle tip changer is not enabled.");
        }
        logger.debug("{}.unloadNozzleTip(): Start", new Object[]{getName()});
        ReferenceNozzleTip nt = (ReferenceNozzleTip) nozzleTip;
        logger.debug("{}.unloadNozzleTip(): moveToSafeZ", new Object[]{getName()});
        moveToSafeZ(1.0);
        logger.debug("{}.unloadNozzleTip(): moveTo End Location", new Object[]{getName()});
        moveTo(nt.getChangerEndLocation(), 1.0);
        logger.debug("{}.unloadNozzleTip(): moveTo Mid Location", new Object[]{getName()});
        moveTo(nt.getChangerMidLocation(), 1.0);
        logger.debug("{}.unloadNozzleTip(): moveTo Start Location", new Object[]{getName()});
        moveTo(nt.getChangerStartLocation(), 0.25);
        moveToSafeZ(1.0);
        logger.debug("{}.unloadNozzleTip(): Finished", new Object[]{getName()});
        nozzleTip = null;
        currentNozzleTipId = null;
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
	    return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        ArrayList<PropertySheetHolder> children = new ArrayList<>();
        children.add(new SimplePropertySheetHolder("Nozzle Tips", getNozzleTips()));
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
		return getName();
	}
    
	public Length getSafeZ() {
		return safeZ;
	}

	public void setSafeZ(Length safeZ) {
		this.safeZ = safeZ;
	}
}
