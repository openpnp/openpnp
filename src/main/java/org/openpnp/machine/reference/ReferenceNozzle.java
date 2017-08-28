package org.openpnp.machine.reference;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.psh.NozzleTipsPropertySheetHolder;
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

    @Attribute(required = false)
    protected boolean invertVacuumSenseLogic;

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

    public boolean isInvertVacuumSenseLogic() {
        return invertVacuumSenseLogic;
    }

    public void setInvertVacuumSenseLogic(boolean invertVacuumSenseLogic) {
        this.invertVacuumSenseLogic = invertVacuumSenseLogic;
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
        Thread.sleep(pickDwellMilliseconds);

        Actuator actuator = getHead().getActuatorByName(vacuumSenseActuatorName);
        if (actuator != null) {
            ReferenceNozzleTip nt = getNozzleTip();
            double vacuumLevel = Double.parseDouble(actuator.read());
            if (invertVacuumSenseLogic) {
                if (vacuumLevel > nt.getVacuumLevelPartOn()) {
                    throw new Exception(String.format(
                            "Pick failure: Vacuum level %f is higher than expected value of %f for part on. Part may have failed to pick.",
                            vacuumLevel, nt.getVacuumLevelPartOn()));
                }
            }
            else {
                if (vacuumLevel < nt.getVacuumLevelPartOn()) {
                    throw new Exception(String.format(
                            "Pick failure: Vacuum level %f is lower than expected value of %f for part on. Part may have failed to pick.",
                            vacuumLevel, nt.getVacuumLevelPartOn()));
                }
            }
        }
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
        Thread.sleep(placeDwellMilliseconds);

        Actuator actuator = getHead().getActuatorByName(vacuumSenseActuatorName);
        if (actuator != null) {
            ReferenceNozzleTip nt = getNozzleTip();
            double vacuumLevel = Double.parseDouble(actuator.read());
            if (invertVacuumSenseLogic) {
                if (vacuumLevel < nt.getVacuumLevelPartOff()) {
                    throw new Exception(String.format(
                            "Place failure: Vacuum level %f is lower than expected value of %f for part off. Part may be stuck to nozzle.",
                            vacuumLevel, nt.getVacuumLevelPartOff()));
                }
            }
            else {
                if (vacuumLevel > nt.getVacuumLevelPartOff()) {
                    throw new Exception(String.format(
                            "Place failure: Vacuum level %f is higher than expected value of %f for part off. Part may be stuck to nozzle.",
                            vacuumLevel, nt.getVacuumLevelPartOff()));
                }
            }
        }
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

        // Check calibration.
        if (nozzleTip != null && nozzleTip.getCalibration().isCalibrationNeeded()) {
            Logger.debug("NozzleTip is not yet calibrated, calibrating now.");
            nozzleTip.getCalibration().calibrate(nozzleTip);
        }

        Logger.debug("{}.moveTo({}, {})", getName(), location, speed);
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
            location = location.subtract(
                    nozzleTip.getCalibration().getCalibratedOffset(location.getRotation()));
            Logger.debug("{}.moveTo({}, {}) (corrected)", getName(), location, speed);
        }
        getDriver().moveTo(this, location, getHead().getMaxPartSpeed() * speed);
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
        
        if (changerEnabled) {
            double speed = getHead().getMachine().getSpeed();
            
            Logger.debug("{}.unloadNozzleTip(): Start", getName());
            ReferenceNozzleTip nt = (ReferenceNozzleTip) nozzleTip;

            Logger.debug("{}.unloadNozzleTip(): moveTo End Location", getName());
            MovableUtils.moveToLocationAtSafeZ(this, nt.getChangerEndLocation(), speed);

            Logger.debug("{}.unloadNozzleTip(): moveTo Mid Location 2", getName());
            moveTo(nt.getChangerMidLocation2(), nt.getChangerMid2ToEndSpeed() * speed);

            Logger.debug("{}.unloadNozzleTip(): moveTo Mid Location", getName());
            moveTo(nt.getChangerMidLocation(), nt.getChangerMidToMid2Speed() * speed);

            Logger.debug("{}.unloadNozzleTip(): moveTo Start Location", getName());
            moveTo(nt.getChangerStartLocation(), nt.getChangerStartToMidSpeed() * speed);
            moveToSafeZ(getHead().getMachine().getSpeed());

            Logger.debug("{}.unloadNozzleTip(): Finished", getName());
        }
        
        nozzleTip = null;
        currentNozzleTipId = null;
        firePropertyChange("nozzleTip", null, getNozzleTip());
        ((ReferenceMachine) head.getMachine()).fireMachineHeadActivity(head);
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
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
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
}
