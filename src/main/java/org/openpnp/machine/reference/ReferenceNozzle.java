package org.openpnp.machine.reference;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.psh.NozzleTipsPropertySheetHolder;
import org.openpnp.machine.reference.wizards.ReferenceNozzleCameraOffsetWizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractNozzle;
import org.openpnp.util.MovableUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

public class ReferenceNozzle extends AbstractNozzle implements ReferenceHeadMountable {
    @Element
    private Location headOffsets = new Location(LengthUnit.Millimeters);

    @Attribute(required = false)
    private int pickDwellMilliseconds;

    @Attribute(required = false)
    private int placeDwellMilliseconds;

    @Attribute(required = false)
    private String currentNozzleTipId;

    @Attribute(required = false)
    private boolean changerEnabled = false;

    @Element(required = false)
    protected Length safeZ = new Length(0, LengthUnit.Millimeters);

    @Element(required = false)
    protected String vacuumSenseActuatorName;
    
    @Deprecated
    @Attribute(required = false)
    protected Boolean invertVacuumSenseLogic = null;

    /**
     * If limitRotation is enabled the nozzle will reverse directions when commanded to rotate past
     * 180 degrees. So, 190 degrees becomes -170 and -190 becomes 170.
     */
    @Attribute(required = false)
    private boolean limitRotation = true;

    protected ReferenceNozzleTip nozzleTip;

    public ReferenceNozzle() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                nozzleTip = (ReferenceNozzleTip) nozzleTips.get(currentNozzleTipId);
            }
        });
    }

    public ReferenceNozzle(String id) {
        this();
        this.id = id;
    }
    
    @Commit
    public void commit() {
        invertVacuumSenseLogic = null;
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

    public String getVacuumSenseActuatorName() {
        return vacuumSenseActuatorName;
    }

    public void setVacuumSenseActuatorName(String vacuumSenseActuatorName) {
        this.vacuumSenseActuatorName = vacuumSenseActuatorName;
    }

    @Override
    public ReferenceNozzleTip getNozzleTip() {
        return nozzleTip;
    }

    @Override
    public void pick(Part part) throws Exception {
        Logger.debug("{}.pick()", getName());
        if (part == null) {
            throw new Exception("Can't pick null part");
        }
        if (nozzleTip == null) {
            throw new Exception("Can't pick, no nozzle tip loaded");
        }
        this.part = part;
        getDriver().pick(this);
        getMachine().fireMachineHeadActivity(head);
        
        // Dwell Time
        Thread.sleep(this.getPickDwellMilliseconds() + nozzleTip.getPickDwellMilliseconds());
    }

    @Override
    public void place() throws Exception {
        Logger.debug("{}.place()", getName());
        if (nozzleTip == null) {
            throw new Exception("Can't place, no nozzle tip loaded");
        }
        getDriver().place(this);
        this.part = null;
        getMachine().fireMachineHeadActivity(head);
        
        // Dwell Time
        Thread.sleep(this.getPlaceDwellMilliseconds() + nozzleTip.getPlaceDwellMilliseconds());
    }

    @Override
    public void moveTo(Location location, double speed) throws Exception {
        // Shortcut Double.NaN. Sending Double.NaN in a Location is an old API that should no
        // longer be used. It will be removed eventually:
        // https://github.com/openpnp/openpnp/issues/255
        // In the mean time, since Double.NaN would cause a problem for calibration, we shortcut
        // it here by replacing any NaN values with the current value from the driver.
        Location currentLocation = getLocation().convertToUnits(location.getUnits());
        if (Double.isNaN(location.getX())) {
            location = location.derive(currentLocation.getX(), null, null, null);
        }
        if (Double.isNaN(location.getY())) {
            location = location.derive(null, currentLocation.getY(), null, null);
        }
        if (Double.isNaN(location.getZ())) {
            location = location.derive(null, null, currentLocation.getZ(), null);
        }
        if (Double.isNaN(location.getRotation())) {
            location = location.derive(null, null, null, currentLocation.getRotation());
        }

        if (limitRotation && !Double.isNaN(location.getRotation())
                && Math.abs(location.getRotation()) > 180) {
            if (location.getRotation() < 0) {
                location = location.derive(null, null, null, location.getRotation() + 360);
            }
            else {
                location = location.derive(null, null, null, location.getRotation() - 360);
            }
        }

        if (nozzleTip != null && nozzleTip.getCalibration().isCalibrated()) {
            Location correctionOffset = nozzleTip.getCalibration().getCalibratedOffset(location.getRotation());
            location = location.subtract(correctionOffset);
            Logger.debug("{}.moveTo({}, {}) (corrected by subtr. offset: {})", getName(), location, speed, correctionOffset);
        } else {
            Logger.debug("{}.moveTo({}, {})", getName(), location, speed);
        }
        ((ReferenceHead) getHead()).moveTo(this, location, getHead().getMaxPartSpeed() * speed);
        getMachine().fireMachineHeadActivity(head);
    }

    @Override
    public void moveToSafeZ(double speed) throws Exception {
        Logger.debug("{}.moveToSafeZ({})", getName(), speed);
        Length safeZ = this.safeZ.convertToUnits(getLocation().getUnits());
        Location l = new Location(getLocation().getUnits(), Double.NaN, Double.NaN,
                safeZ.getValue(), Double.NaN);
        getDriver().moveTo(this, l, getHead().getMaxPartSpeed() * speed);
        getMachine().fireMachineHeadActivity(head);
    }

    @Override
    public void loadNozzleTip(NozzleTip nozzleTip) throws Exception {
        if (this.nozzleTip == nozzleTip) {
            return;
        }

        ReferenceNozzleTip nt = (ReferenceNozzleTip) nozzleTip;

        if (changerEnabled) {
            double speed = getHead().getMachine().getSpeed();
            
            unloadNozzleTip();
            Logger.debug("{}.loadNozzleTip({}): Start", getName(), nozzleTip.getName());
            
            try {
                Map<String, Object> globals = new HashMap<>();
                globals.put("head", getHead());
                globals.put("nozzle", this);
                globals.put("nozzleTip", nt);
                Configuration.get()
                             .getScripting()
                             .on("NozzleTip.BeforeLoad", globals);
            }
            catch (Exception e) {
                Logger.warn(e);
            }

            Logger.debug("{}.loadNozzleTip({}): moveTo Start Location",
                    new Object[] {getName(), nozzleTip.getName()});
            MovableUtils.moveToLocationAtSafeZ(this, nt.getChangerStartLocation(), speed);

            Logger.debug("{}.loadNozzleTip({}): moveTo Mid Location",
                    new Object[] {getName(), nozzleTip.getName()});
            moveTo(nt.getChangerMidLocation(), nt.getChangerStartToMidSpeed() * speed);

            Logger.debug("{}.loadNozzleTip({}): moveTo Mid Location 2",
                    new Object[] {getName(), nozzleTip.getName()});
            moveTo(nt.getChangerMidLocation2(), nt.getChangerMidToMid2Speed() * speed);

            Logger.debug("{}.loadNozzleTip({}): moveTo End Location",
                    new Object[] {getName(), nozzleTip.getName()});
            moveTo(nt.getChangerEndLocation(), nt.getChangerMid2ToEndSpeed() * speed);
            moveToSafeZ(getHead().getMachine().getSpeed());

            Logger.debug("{}.loadNozzleTip({}): Finished",
                    new Object[] {getName(), nozzleTip.getName()});
            
            try {
                Map<String, Object> globals = new HashMap<>();
                globals.put("head", getHead());
                globals.put("nozzle", this);
                Configuration.get()
                             .getScripting()
                             .on("NozzleTip.Loaded", globals);
            }
            catch (Exception e) {
                Logger.warn(e);
            }
        }
        
        this.nozzleTip = nt;
        this.nozzleTip.getCalibration().reset();
        currentNozzleTipId = nozzleTip.getId();
        firePropertyChange("nozzleTip", null, getNozzleTip());
        ((ReferenceMachine) head.getMachine()).fireMachineHeadActivity(head);
    }

    @Override
    public void unloadNozzleTip() throws Exception {
        if (nozzleTip == null) {
            return;
        }
        
        double speed = getHead().getMachine().getSpeed();
        
        Logger.debug("{}.unloadNozzleTip(): Start", getName());
        ReferenceNozzleTip nt = (ReferenceNozzleTip) nozzleTip;

        try {
            Map<String, Object> globals = new HashMap<>();
            globals.put("head", getHead());
            globals.put("nozzle", this);
            globals.put("nozzleTip", nt);
            Configuration.get()
                         .getScripting()
                         .on("NozzleTip.BeforeUnload", globals);
        }
        catch (Exception e) {
            Logger.warn(e);
        }

        Logger.debug("{}.unloadNozzleTip(): moveTo End Location", getName());
        MovableUtils.moveToLocationAtSafeZ(this, nt.getChangerEndLocation(), speed);

        if (changerEnabled) {
            Logger.debug("{}.unloadNozzleTip(): moveTo Mid Location 2", getName());
            moveTo(nt.getChangerMidLocation2(), nt.getChangerMid2ToEndSpeed() * speed);

            Logger.debug("{}.unloadNozzleTip(): moveTo Mid Location", getName());
            moveTo(nt.getChangerMidLocation(), nt.getChangerMidToMid2Speed() * speed);

            Logger.debug("{}.unloadNozzleTip(): moveTo Start Location", getName());
            moveTo(nt.getChangerStartLocation(), nt.getChangerStartToMidSpeed() * speed);
            moveToSafeZ(getHead().getMachine().getSpeed());

            Logger.debug("{}.unloadNozzleTip(): Finished", getName());
            
            try {
                Map<String, Object> globals = new HashMap<>();
                globals.put("head", getHead());
                globals.put("nozzle", this);
                Configuration.get()
                             .getScripting()
                             .on("NozzleTip.Unloaded", globals);
            }
            catch (Exception e) {
                Logger.warn(e);
            }
        }
        
        nozzleTip = null;
        currentNozzleTipId = null;
        firePropertyChange("nozzleTip", null, getNozzleTip());
        ((ReferenceMachine) head.getMachine()).fireMachineHeadActivity(head);
        
        if (!changerEnabled) {
            throw new Exception("Manual NozzleTip change required!");
        }
    }

    @Override
    public Location getLocation() {
        Location location = getDriver().getLocation(this);
        if (nozzleTip != null && nozzleTip.getCalibration().isCalibrated()) {
            Location offset =
                    nozzleTip.getCalibration().getCalibratedOffset(location.getRotation());
            location = location.add(offset);
        }
        return location;
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
        children.add(new NozzleTipsPropertySheetHolder(this, "Nozzle Tips", getNozzleTips(), null));
        return children.toArray(new PropertySheetHolder[] {});
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard()),
                new PropertySheetWizardAdapter(new ReferenceNozzleCameraOffsetWizard(this), "Offset Wizard")
        };
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] {deleteAction};
    }

    public Action deleteAction = new AbstractAction("Delete Nozzle") {
        {
            putValue(SMALL_ICON, Icons.nozzleRemove);
            putValue(NAME, "Delete Nozzle");
            putValue(SHORT_DESCRIPTION, "Delete the currently selected nozzle.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (getHead().getNozzles().size() == 1) {
                MessageBoxes.errorBox(null, "Error: Nozzle Not Deleted", "Can't delete last nozzle. There must be at least one nozzle.");
                return;
            }
            int ret = JOptionPane.showConfirmDialog(MainFrame.get(),
                    "Are you sure you want to delete " + getName() + "?",
                    "Delete " + getName() + "?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                getHead().removeNozzle(ReferenceNozzle.this);
            }
        }
    };

    @Override
    public String toString() {
        return getName() + " " + getId();
    }

    public Length getSafeZ() {
        return safeZ;
    }

    public void setSafeZ(Length safeZ) {
        this.safeZ = safeZ;
    }

    @Override
    public void moveTo(Location location) throws Exception {
        moveTo(location, getHead().getMachine().getSpeed());
    }

    @Override
    public void moveToSafeZ() throws Exception {
        moveToSafeZ(getHead().getMachine().getSpeed());
    }

    ReferenceDriver getDriver() {
        return getMachine().getDriver();
    }

    ReferenceMachine getMachine() {
        return (ReferenceMachine) Configuration.get().getMachine();
    }
    
    @Override
    public boolean isPartDetectionEnabled() {
        return vacuumSenseActuatorName != null && !vacuumSenseActuatorName.isEmpty();
    }
    
    private double readVacuumLevel() throws Exception {
        Actuator actuator = getHead().getActuatorByName(vacuumSenseActuatorName);
        if (actuator == null) {
            throw new Exception(String.format("Can't find vacuum actuator %s", vacuumSenseActuatorName));
        }
        return Double.parseDouble(actuator.read());
    }

    @Override
    public boolean isPartOn() throws Exception {
        ReferenceNozzleTip nt = getNozzleTip();
        double vacuumLevel = readVacuumLevel();
        return vacuumLevel >= nt.getVacuumLevelPartOnLow() && vacuumLevel <= nt.getVacuumLevelPartOnHigh();
    }

    @Override
    public boolean isPartOff() throws Exception {
        ReferenceNozzleTip nt = getNozzleTip();
        double vacuumLevel = readVacuumLevel();
        return vacuumLevel >= nt.getVacuumLevelPartOffLow() && vacuumLevel <= nt.getVacuumLevelPartOffHigh();
    }
}
