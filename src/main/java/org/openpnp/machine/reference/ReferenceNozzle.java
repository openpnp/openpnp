package org.openpnp.machine.reference;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceNozzleTip.VacuumMeasurementMethod;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.camera.ReferenceCamera;
import org.openpnp.machine.reference.solutions.ActuatorSolutions;
import org.openpnp.machine.reference.wizards.ReferenceNozzleCameraOffsetWizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleCompatibleNozzleTipsWizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleConfigurationWizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleToolChangerWizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleVacuumWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Actuator.ActuatorValueType;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Camera.Looking;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.JobProcessor;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.spi.base.AbstractNozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.SimpleGraph;
import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persist;

public class ReferenceNozzle extends AbstractNozzle implements HeadMountable {
    public static class ManualUnloadException extends JobProcessor.JobProcessorException {
        private static final long serialVersionUID = 1L;
    
        public ManualUnloadException(Object source, String message) {
            super(source, message, true);
        }
    }

    public static class ManualLoadException extends JobProcessor.JobProcessorException {
        private static final long serialVersionUID = 1L;
    
        public ManualLoadException(Object source, Throwable throwable) {
            super(source, throwable, true);
        }
    }

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

    @Attribute(required = false)
    private boolean nozzleTipChangedOnManualFeed = false;

    @Element(required = false)
    private Location manualNozzleTipChangeLocation = new Location(LengthUnit.Millimeters);

    @Deprecated
    @Element(required = false)
    protected Length safeZ = null;

    @Attribute(required = false)
    private boolean enableDynamicSafeZ = false;

    @Element(required = false)
    private String vacuumSenseActuatorName;

    @Element(required = false)
    private String vacuumActuatorName;

    @Element(required = false)
    private String blowOffActuatorName;

    @Attribute(required = false)
    private boolean blowOffClosingValve = true;

    @Attribute(required = false)
    private int version; // the OpenPnP target version/migration status (version x 100)

    @Deprecated
    @Attribute(required = false)
    private boolean limitRotation = true;

    private Actuator vacuumSenseActuator;
    private Actuator vacuumActuator;
    private Actuator blowOffActuator;

    protected ReferenceNozzleTip nozzleTip;

    public ReferenceNozzle() {
        super();
    }
    public ReferenceNozzle(String id) {
        this();
        this.id = id;
    }

    @Override
    public void applyConfiguration(Configuration configuration) {
        super.applyConfiguration(configuration);
        // When brand new nozzles are created rather than loaded from configuration, configurationLoaded() is also 
        // triggered. Therefore we need to check for the presence of the head. 
        if (getHead() !=  null) {
            // Resolve nozzle tip.
            nozzleTip = (ReferenceNozzleTip) configuration.getMachine().getNozzleTip(currentNozzleTipId);

            // Resolve the actuators.
            vacuumSenseActuator = getHead().getActuatorByName(vacuumSenseActuatorName); 
            vacuumActuator = getHead().getActuatorByName(vacuumActuatorName); 
            blowOffActuator = getHead().getActuatorByName(blowOffActuatorName); 

            if (version < 200) {
                // Migration of these actuators has gone back and forth, cumbersome resolution needed. 
                if (vacuumSenseActuator == null) {
                    vacuumSenseActuator = vacuumActuator;
                }
                else if (vacuumActuator == null) {
                    vacuumActuator = vacuumSenseActuator;
                }
                if (blowOffActuator != null) {
                    // Type the vacuum and the blow off actuators (typical use).
                    AbstractActuator.suggestValueType(vacuumActuator, ActuatorValueType.Double);
                    AbstractActuator.suggestValueType(blowOffActuator, ActuatorValueType.Double);
                }
                // Migration is done.
                version = 200;
            }

            if (isManualNozzleTipChangeLocationUndefined()) {
                // try to clone from other nozzle. 
                try {
                    for (Nozzle nozzle : configuration.getMachine().getDefaultHead().getNozzles()) {
                        if (nozzle instanceof ReferenceNozzle) {
                            if (!((ReferenceNozzle) nozzle).isManualNozzleTipChangeLocationUndefined()) {
                                manualNozzleTipChangeLocation = ((ReferenceNozzle) nozzle).getManualNozzleTipChangeLocation();
                                break;
                            }
                        }
                    }
                }
                catch (Exception e) {
                }
            }
            if (isManualNozzleTipChangeLocationUndefined()) {
                // Migrate from old setting.
                for (NozzleTip nt : configuration.getMachine().getNozzleTips()) {
                    if (nt instanceof ReferenceNozzleTip) { 
                        manualNozzleTipChangeLocation = ((ReferenceNozzleTip) nt).getChangerEndLocation();
                        break;
                    }
                }
            }
        }
    }

    @Persist
    protected void persist() {
        // Make sure the latest actuator names are persisted.
        vacuumSenseActuatorName = (vacuumSenseActuator == null ? null : vacuumSenseActuator.getName());
        vacuumActuatorName = (vacuumActuator == null ? null : vacuumActuator.getName());
        blowOffActuatorName = (blowOffActuator == null ? null : blowOffActuator.getName());
    }

    @Deprecated
    public boolean isLimitRotation() {
        return limitRotation;
    }

    public boolean isEnableDynamicSafeZ() {
        return enableDynamicSafeZ;
    }

    public void setEnableDynamicSafeZ(boolean enableDynamicSafeZ) {
        this.enableDynamicSafeZ = enableDynamicSafeZ;
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
        Location oldValue = this.headOffsets;
        this.headOffsets = headOffsets;
        firePropertyChange("headOffsets", oldValue, headOffsets);
        adjustHeadOffsetsDependencies(oldValue, headOffsets);
    }

    /**
     * Adjust any dependent head offsets, e.g. after calibration.
     * 
     * @param headOffsetsOld
     * @param headOffsetsNew
     * @param offsetsDiff
     */
    private void adjustHeadOffsetsDependencies(Location headOffsetsOld, Location headOffsetsNew) {
        Location offsetsDiff = headOffsetsNew.subtract(headOffsetsOld).convertToUnits(LengthUnit.Millimeters);
        if (offsetsDiff.getLinearDistanceTo(Location.origin) > 0.01) {
            // Changing a X, Y head offset invalidates the nozzle tip calibration. Just changing Z leaves it intact. 
            ReferenceNozzleTipCalibration.resetAllNozzleTips();
        }
        if (offsetsDiff.isInitialized() && headOffsetsNew.isInitialized() && head != null) {
            if (manualNozzleTipChangeLocation.isInitialized()) {
                Location oldLocation = manualNozzleTipChangeLocation;
                setManualNozzleTipChangeLocation(oldLocation.add(offsetsDiff));
                Logger.info("Set manual nozzle tip change location for " + getName() + " to " + manualNozzleTipChangeLocation
                        + " (previously " + oldLocation + ")");
            }
            if (headOffsetsOld.isInitialized()) {
                // The old offsets were not zero, adjust some dependent head offsets.

                // Where another HeadMountable, such as an Actuator, is fastened to the nozzle, it may have the same X, Y head offsets, i.e. these were very 
                // likely copied over, like customary for the ReferencePushPullFeeder actuator. Adjust them likewise. 
                for (HeadMountable hm : head.getHeadMountables()) {
                    if (this != hm 
                            && !(hm instanceof ReferenceNozzle)) {
                        Location otherHeadOffsets = hm.getHeadOffsets();
                        if (otherHeadOffsets.isInitialized() 
                                && headOffsetsOld.convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(otherHeadOffsets) <= 0.01) {
                            // Take X, Y (but not Z).
                            Location hmOffsets = otherHeadOffsets.derive(headOffsetsNew, true, true, false, false);
                            Logger.info("Set "+hm.getClass().getSimpleName()+" " + hm.getName() + " head offsets to " + hmOffsets
                                    + " (previously " + otherHeadOffsets + ")");
                            hm.setHeadOffsets(hmOffsets);
                        }
                    }
                }

                // Also adjust up-looking camera offsets, as these were very likely calibrated using the default nozzle.
                try {
                    if (this == head.getDefaultNozzle()) {
                        for (Camera camera : getMachine().getCameras()) {
                            if (camera instanceof ReferenceCamera 
                                    && camera.getLooking() == Looking.Up) {
                            	HeadMountable upLookingCamera = (HeadMountable) camera;
                                Location cameraOffsets = upLookingCamera.getHeadOffsets();
                                if (cameraOffsets.isInitialized()) {
                                    cameraOffsets = cameraOffsets.add(offsetsDiff);
                                    Logger.info("Set camera " + upLookingCamera.getName() + " head offsets to " + cameraOffsets
                                            + " (previously " + upLookingCamera.getHeadOffsets() + ")");
                                    upLookingCamera.setHeadOffsets(cameraOffsets);
                                }
                            }
                        }
                    }
                }
                catch (Exception e) {
                    Logger.warn(e);
                }
            }
        }
    }

    @Override
    public ReferenceNozzleTip getNozzleTip() {
        return nozzleTip;
    }

    public void setNozzleTip(ReferenceNozzleTip nozzleTip) {
        Object oldValue = this.nozzleTip;
        currentNozzleTipId = (nozzleTip != null ? nozzleTip.getId() : null);
        this.nozzleTip = nozzleTip;
        firePropertyChange("nozzleTip", oldValue, nozzleTip);
        ((ReferenceMachine) head.getMachine()).fireMachineHeadActivity(head);
    }

    @Override
    public boolean isNozzleTipChangedOnManualFeed() {
        return nozzleTipChangedOnManualFeed;
    }

    public void setNozzleTipChangedOnManualFeed(boolean nozzleTipChangedOnManualFeed) {
        this.nozzleTipChangedOnManualFeed = nozzleTipChangedOnManualFeed;
    }

    public Location getManualNozzleTipChangeLocation() {
        return manualNozzleTipChangeLocation;
    }

    public void setManualNozzleTipChangeLocation(Location manualNozzleTipChangeLocation) {
        Object oldValue = this.manualNozzleTipChangeLocation;
        this.manualNozzleTipChangeLocation = manualNozzleTipChangeLocation;
        firePropertyChange("manualNozzleTipChangeLocation", oldValue, manualNozzleTipChangeLocation);
    }

    @Override
    public void moveToPickLocation(Feeder feeder) throws Exception {
        // The default ReferenceNozzle implementation just moves to the feeder part pickLocation at safe Z.
        // But see Overrides such ContactProbeNozzle.
        Location pickLocation = feeder.getPickLocation();
        if (feeder.isPartHeightAbovePickLocation()) {
            Length partHeight = getSafePartHeight(feeder.getPart());
            pickLocation = pickLocation.add(new Location(partHeight.getUnits(), 0, 0, partHeight.getValue(), 0));
        }
        MovableUtils.moveToLocationAtSafeZ(this, pickLocation);
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

        Map<String, Object> globals = new HashMap<>();
        globals.put("nozzle", this);
        globals.put("part", part);
        Configuration.get().getScripting().on("Nozzle.BeforePick", globals);

        setPart(part);

        // if the method needs it, store one measurement up front
        storeBeforePickVacuumLevel();

        double pickVacuumThreshold = part.getPackage().getPickVacuumLevel();
        if (Double.compare(pickVacuumThreshold, Double.valueOf(0.0)) != 0) {
            actuateVacuumValve(pickVacuumThreshold);
        } 
        else {
            actuateVacuumValve(true);
        }

        // wait for the Dwell Time and/or make sure the vacuum level builds up to the desired range (with timeout)
        establishPickVacuumLevel(this.getPickDwellMilliseconds() + nozzleTip.getPickDwellMilliseconds());

        getMachine().fireMachineHeadActivity(head);

        Configuration.get().getScripting().on("Nozzle.AfterPick", globals);
    }

    @Override
    public void moveToPlacementLocation(Location placementLocation, Part part) throws Exception {
        // The default ReferenceNozzle implementation just moves to the placementLocation + partHeight at safe Z.
        if (part != null) {
            placementLocation = placementLocation
                    .add(new Location(part.getHeight().getUnits(), 0, 0, part.getHeight().getValue(), 0));
        }
        MovableUtils.moveToLocationAtSafeZ(this, placementLocation);
    }

    @Override
    public void place() throws Exception {
        Logger.debug("{}.place()", getName());
        if (nozzleTip == null) {
            throw new Exception("Can't place, no nozzle tip loaded");
        }

        Map<String, Object> globals = new HashMap<>();
        globals.put("nozzle", this);
        globals.put("part", getPart());
        Configuration.get().getScripting().on("Nozzle.BeforePlace", globals);

        // if the method needs it, store one measurement up front
        storeBeforePlaceVacuumLevel();

        double placeBlowLevel = nozzleTip.getPlaceBlowOffLevel();
        if (getPart() != null 
                && getPart().getPackage().getPlaceBlowOffLevel() != 0) {
            placeBlowLevel = getPart().getPackage().getPlaceBlowOffLevel();
        }
        if (placeBlowLevel != 0) {
            actuateBlowValve(placeBlowLevel);
            if (!blowOffClosingValve) {
                actuateVacuumValve(false);
            }
        }
        else {
            actuateVacuumValve(false);
        }

        // wait for the Dwell Time and/or make sure the vacuum level decays to the desired range (with timeout)
        establishPlaceVacuumLevel(this.getPlaceDwellMilliseconds() + nozzleTip.getPlaceDwellMilliseconds());

        setPart(null);
        getMachine().fireMachineHeadActivity(head);

        Configuration.get().getScripting().on("Nozzle.AfterPlace", globals);
    }

    protected ReferenceNozzleTip getUnloadedNozzleTipStandin() {
        for (NozzleTip nozzleTip : this.getCompatibleNozzleTips()) {
            if (nozzleTip instanceof ReferenceNozzleTip) {
                ReferenceNozzleTip referenceNozzleTip = (ReferenceNozzleTip)nozzleTip;
                if (referenceNozzleTip.isUnloadedNozzleTipStandin()) {
                    return referenceNozzleTip;
                }
            }
        }
        return null;
    }
    
    public ReferenceNozzleTip getCalibrationNozzleTip() {
        if (nozzleTip != null) {
            // normally we have the loaded nozzle tip as the calibration nozzle tip
            ReferenceNozzleTip calibrationNozzleTip = null;
            if (nozzleTip instanceof ReferenceNozzleTip) {
                calibrationNozzleTip = (ReferenceNozzleTip)nozzleTip;
            }
            return calibrationNozzleTip;
        } else {
            // if no tip is mounted, we use the "unloaded" nozzle tip stand-in, so we 
            // can still calibrate
            return getUnloadedNozzleTipStandin();
        }
    }

    @Override
    public Location getCameraToolCalibratedOffset(Camera camera) {
        // Apply the axis offset from runout calibration here. 
        ReferenceNozzleTip calibrationNozzleTip = getCalibrationNozzleTip();
        if (calibrationNozzleTip != null && calibrationNozzleTip.getCalibration().isCalibrated(this)) {
            return calibrationNozzleTip.getCalibration().getCalibratedCameraOffset(this, camera);
        }

        return new Location(camera.getUnitsPerPixel().getUnits());
    }

    @Override
    public void calibrate() throws Exception {
        ReferenceNozzleTip calibrationNozzleTip = getCalibrationNozzleTip();
        if (calibrationNozzleTip != null) {
            calibrationNozzleTip.getCalibration().calibrate(this);
        }
    }
    
    @Override
    public boolean isCalibrated() {
        ReferenceNozzleTip calibrationNozzleTip = getCalibrationNozzleTip();
        if (calibrationNozzleTip != null) {
            return calibrationNozzleTip.getCalibration().isCalibrated(this);
        }
        // No calibration needed.
        return true;
    }

    @Override
    public Location toHeadLocation(Location location, Location currentLocation, LocationOption... options) {
        boolean quiet = Arrays.asList(options).contains(LocationOption.Quiet);
        // Check SuppressCompensation, in that case disable nozzle calibration
        if (! Arrays.asList(options).contains(LocationOption.SuppressDynamicCompensation)) {
            // Apply the rotationModeOffset.
            if (rotationModeOffset != null) { 
                location = location.subtractWithRotation(new Location(location.getUnits(), 0, 0, 0, rotationModeOffset));
                if (!quiet) {
                    Logger.trace("{}.toHeadLocation({}, ...) rotation mode offset {}", getName(), location, rotationModeOffset);
                }
            }
            // Apply runout compensation.
            ReferenceNozzleTip calibrationNozzleTip = getCalibrationNozzleTip();
            if (calibrationNozzleTip != null && calibrationNozzleTip.getCalibration().isCalibrated(this)) {
                Location correctionOffset = calibrationNozzleTip.getCalibration().getCalibratedOffset(this, location.getRotation());
                location = location.subtract(correctionOffset);
                if (!quiet) {
                    Logger.trace("{}.toHeadLocation({}, ...) runout compensation {}", getName(), location, correctionOffset);
                }
            }
        }
        return super.toHeadLocation(location, currentLocation, options);
    }

    @Override
    public Location toHeadMountableLocation(Location location, Location currentLocation, LocationOption... options) {
        location = super.toHeadMountableLocation(location, currentLocation, options);
        // Unapply runout compensation.
        // Check SuppressCompensation, in that case disable nozzle calibration.
        if (! Arrays.asList(options).contains(LocationOption.SuppressDynamicCompensation)) {
            ReferenceNozzleTip calibrationNozzleTip = getCalibrationNozzleTip();
            if (calibrationNozzleTip != null && calibrationNozzleTip.getCalibration().isCalibrated(this)) {
                Location offset =
                        calibrationNozzleTip.getCalibration().getCalibratedOffset(this, location.getRotation());
                location = location.add(offset);
            }
            // Unapply the rotationModeOffset.
            if (rotationModeOffset != null) { 
                location = location.addWithRotation(new Location(location.getUnits(), 
                        0, 0, 0, rotationModeOffset));
            }
        }
        return location;
    }

    @Override
    public Length getSafePartHeight(Part part) {
        if (part != null) {
            if (part.isPartHeightUnknown() && nozzleTip != null) {
                return nozzleTip.getMaxPartHeight();
            }
            else {
                return part.getHeight();
            }
        }
        return new Length(0, LengthUnit.Millimeters);
    }

    @Override 
    public Length getEffectiveSafeZ() throws Exception {
        Length safeZ = super.getEffectiveSafeZ();
        if (safeZ == null) {
            throw new Exception("Nozzle "+getName()+" has no Z axis with Safe Zone mapped.");
        }
        if (enableDynamicSafeZ) { 
            // if a part is loaded, decrease (higher) safeZ
            safeZ = safeZ.add(getSafePartHeight());
            // Note, the safeZ value will be validated in moveToSafeZ()
            // to make sure it is not outside the Safe Z Zone.
        }
        return safeZ;
    }

    @Override
    public void home() throws Exception {
        Logger.debug("{}.home()", getName());
        for (NozzleTip attachedNozzleTip : this.getCompatibleNozzleTips()) {
            if (attachedNozzleTip instanceof ReferenceNozzleTip) {
                ReferenceNozzleTip calibrationNozzleTip = (ReferenceNozzleTip)attachedNozzleTip;
                if (calibrationNozzleTip.getCalibration().isRecalibrateOnHomeNeeded(this)) {
                    if (calibrationNozzleTip == this.getCalibrationNozzleTip()) {
                        // The currently mounted nozzle tip.
                        try {
                            Logger.debug("{}.home() nozzle tip {} calibration neeeded", getName(), calibrationNozzleTip.getName());
                            calibrationNozzleTip.getCalibration().calibrate(this, true, false);
                        }
                        catch (Exception e) {
                            if (calibrationNozzleTip.getCalibration().isFailHoming()) {
                                throw e; 
                            }
                            else {
                                UiUtils.messageBoxOnExceptionLater(() -> {
                                    throw e;
                                });
                            }
                        }
                    }
                    else {
                        // Not currently mounted so just reset.
                        Logger.debug("{}.home() nozzle tip {} calibration reset", getName(), calibrationNozzleTip.getName());
                        calibrationNozzleTip.getCalibration().reset(this);
                    }
                }
            }
        }
    }

    @Override
    public void loadNozzleTip(NozzleTip nozzleTip, boolean withCalibration) throws Exception {
        // if the requested nozzle-tip is already loaded, skip the load step, but continue as calibration might be required.
        if (this.nozzleTip != nozzleTip) {
            if (getPart() != null) {
                throw new Exception("Nozzle "+getName()+" still has a part loaded. Please discard first.");
            }

            // Make sure there is no rotation offset still applied.
            setRotationModeOffset(null);

            ReferenceNozzleTip nt = (ReferenceNozzleTip) nozzleTip;

            if (!getCompatibleNozzleTips().contains(nt)) {
                throw new Exception("Can't load incompatible nozzle tip.");
            }

            // compose instructions for manual nozzle tip changing on the fly while catching unload exceptions
            String manualChangeInstructions = "Task interrupted: Please perform";

            ReferenceNozzle n = nt.getNozzleWhereLoaded();  // remember the nozzle to generate change instructions, if needed
            if (nt.getNozzleWhereLoaded() != null) {
                // Nozzle tip is on different nozzle - unload it from there first.
                try {
                    nt.getNozzleWhereLoaded().unloadNozzleTip();
                } catch (ManualUnloadException e) {
                    // combine this unload exception with following unload/load exceptions into one
                    // There is code behind the exception that may calibrate the bare nozzle, which is
                    // not executed due to the exception.
                    manualChangeInstructions += "\na manual nozzle tip " + nt.getName() + " unload from nozzle " + n.getName() + " and";
                }
            }

            ReferenceNozzleTip nt2 = getNozzleTip();  // remember the nozzle tip currently loaded to generate change instructions, if needed
            try {
                unloadNozzleTip();
            } catch (ManualUnloadException e) {
                // combine this unload exception with following unload/load exceptions into one
                // There is code behind the exception that may calibrate the bare nozzle, which is
                // not executed due to the exception.
                manualChangeInstructions += "\na manual nozzle tip " + nt2.getName() + " unload from nozzle " + getName() + " and";
            }

            double speed = getHead().getMachine().getSpeed();
            if (!nt.isUnloadedNozzleTipStandin()) {
                if (changerEnabled) {
                    Logger.debug("{}.loadNozzleTip({}): Start", getName(), nozzleTip.getName());

                    Map<String, Object> globals = new HashMap<>();
                    globals.put("head", getHead());
                    globals.put("nozzle", this);
                    globals.put("nozzleTip", nt);

                    Configuration.get()
                    .getScripting()
                    .on("NozzleTip.BeforeLoad", globals);

                    ensureZCalibrated(true);

                    Location startLocation = nt.getChangerStartLocationCalibrated(true);
                    if (startLocation.isInitialized()) {
                        Logger.debug("{}.loadNozzleTip({}): moveTo Start Location", getName(), nozzleTip.getName());
                        MovableUtils.moveToLocationAtSafeZ(this, startLocation, speed);
                    }

                    Actuator tcPostOneActuator = getMachine().getActuatorByName(nt.getChangerActuatorPostStepOne());
                    if (tcPostOneActuator != null) {
                        tcPostOneActuator.actuate(true);
                    }

                    Location midLocation = nt.getChangerMidLocationCalibrated(false);
                    if (midLocation.isInitialized()) {
                        Logger.debug("{}.loadNozzleTip({}): moveTo Mid Location", getName(), nozzleTip.getName());
                        moveTo(midLocation, nt.getChangerStartToMidSpeed() * speed);
                    }

                    Actuator tcPostTwoActuator = getMachine().getActuatorByName(nt.getChangerActuatorPostStepTwo());
                    if (tcPostTwoActuator !=null) {
                        tcPostTwoActuator.actuate(true);
                    }

                    Location midLocation2 = nt.getChangerMidLocation2Calibrated(false);
                    if (midLocation2.isInitialized()) {
                        Logger.debug("{}.loadNozzleTip({}): moveTo Mid Location 2", getName(), nozzleTip.getName());
                        moveTo(midLocation2, nt.getChangerMidToMid2Speed() * speed);
                    }

                    Actuator tcPostThreeActuator = getMachine().getActuatorByName(nt.getChangerActuatorPostStepThree());
                    if (tcPostThreeActuator !=null) {
                        tcPostThreeActuator.actuate(true);
                    }

                    Location endLocation = nt.getChangerEndLocationCalibrated(false);
                    if (endLocation.isInitialized()) {
                        Logger.debug("{}.loadNozzleTip({}): moveTo End Location", getName(), nozzleTip.getName());
                        moveTo(endLocation, nt.getChangerMid2ToEndSpeed() * speed);
                    }
                    moveToSafeZ(getHead().getMachine().getSpeed());

                    Logger.debug("{}.loadNozzleTip({}): Finished", getName(), nozzleTip.getName());

                    Configuration.get()
                    .getScripting()
                    .on("NozzleTip.Loaded", globals);
                }
                else {
                    Logger.debug("{}.loadNozzleTip({}): moveTo manual Location", getName(), nozzleTip.getName());
                    assertManualChangeLocation();
                    MovableUtils.moveToLocationAtSafeZ(this, getManualNozzleTipChangeLocation());
                }
            }

            setNozzleTip(nt);

            // depending on configuration, set the nozzle tip now installed to be uncalibrated
            if (this.nozzleTip.getCalibration().isRecalibrateOnNozzleTipChangeNeeded(this) 
                    || this.nozzleTip.getCalibration().isRecalibrateOnNozzleTipChangeInJobNeeded(this)) {
                Logger.debug("{}.loadNozzleTip() nozzle tip {} calibration reset", getName(), this.nozzleTip.getName());
                // can't automatically recalibrate with manual change - reset() for now
                this.nozzleTip.getCalibration().reset(this);
            }

            if (!nt.isUnloadedNozzleTipStandin() && !changerEnabled) {
                waitForCompletion(CompletionType.WaitForStillstand);
                manualChangeInstructions += "\na manual nozzle tip " + nt.getName()+" load on nozzle "+getName()+" now.";
                manualChangeInstructions += "\nWhen you press OK, the nozzle tip will be calibrated (if enabled).";
                throw new ManualLoadException(this, 
                        new UiUtils.ExceptionWithContinuation(manualChangeInstructions,  () -> { loadNozzleTip(nozzleTip, withCalibration); }));
            }
        }

        ensureZCalibrated(true);
        
        if (withCalibration
                && this.nozzleTip.getCalibration().isRecalibrateOnNozzleTipChangeNeeded(this)
                && !this.nozzleTip.getCalibration().isCalibrated(this)) {
            Logger.debug("{}.loadNozzleTip() nozzle tip {} calibration needed", getName(), this.nozzleTip.getName());
            this.nozzleTip.getCalibration().calibrate(this);
        }
    }
    
    @Override
    public void unloadNozzleTip() throws Exception {
        if (getPart() != null) {
            throw new Exception("Nozzle "+getName()+" still has a part loaded. Please discard first.");
        }

        // Make sure there is no rotation offset still applied.
        setRotationModeOffset(null);

        // if this nozzle is already empty, skip the unload procedure, but continue to eventually calibration the bare nozzle
        if (nozzleTip != null) {
            ReferenceNozzleTip nt = (ReferenceNozzleTip) nozzleTip;

            if (!nt.isUnloadedNozzleTipStandin()) {
                Logger.debug("{}.unloadNozzleTip(): Start", getName());

                double speed = getHead().getMachine().getSpeed();

                if (changerEnabled) {
                    Map<String, Object> globals = new HashMap<>();
                    globals.put("head", getHead());
                    globals.put("nozzle", this);
                    globals.put("nozzleTip", nt);
                    Configuration.get()
                    .getScripting()
                    .on("NozzleTip.BeforeUnload", globals);

                    ensureZCalibrated(false);


                    Location endLocation = nt.getChangerEndLocationCalibrated(true);
                    if (endLocation.isInitialized()) {
                        Logger.debug("{}.unloadNozzleTip(): moveTo End Location", getName());
                        MovableUtils.moveToLocationAtSafeZ(this, endLocation, speed);
                    }

                    Actuator tcPostThreeActuator = getMachine().getActuatorByName(nt.getChangerActuatorPostStepThree());
                    if (tcPostThreeActuator !=null) {
                        tcPostThreeActuator.actuate(false);
                    }

                    Location midLocation2 = nt.getChangerMidLocation2Calibrated(false);
                    if (midLocation2.isInitialized()) {
                        Logger.debug("{}.unloadNozzleTip(): moveTo Mid Location 2", getName());
                        moveTo(midLocation2, nt.getChangerMid2ToEndSpeed() * speed);
                    }

                    Actuator tcPostTwoActuator = getMachine().getActuatorByName(nt.getChangerActuatorPostStepTwo());
                    if (tcPostTwoActuator !=null) {
                        tcPostTwoActuator.actuate(false);
                    }

                    Location midLocation = nt.getChangerMidLocationCalibrated(false);
                    if (midLocation.isInitialized()) {
                        Logger.debug("{}.unloadNozzleTip(): moveTo Mid Location", getName());
                        moveTo(midLocation, nt.getChangerMidToMid2Speed() * speed);
                    }

                    Actuator tcPostOneActuator = getMachine().getActuatorByName(nt.getChangerActuatorPostStepOne());
                    if (tcPostOneActuator != null) {
                        tcPostOneActuator.actuate(false);
                    }

                    Location startLocation = nt.getChangerStartLocationCalibrated(false);
                    if (startLocation.isInitialized()) {
                        Logger.debug("{}.unloadNozzleTip(): moveTo Start Location", getName());
                        moveTo(startLocation, nt.getChangerStartToMidSpeed() * speed);
                    }
                    moveToSafeZ(getHead().getMachine().getSpeed());

                    Logger.debug("{}.unloadNozzleTip(): Finished", getName());

                    Configuration.get()
                    .getScripting()
                    .on("NozzleTip.Unloaded", globals);
                }
                else {
                    Logger.debug("{}.unloadNozzleTip({}): moveTo manual Location",
                            new Object[] {getName(), nozzleTip.getName()});
                    assertManualChangeLocation();
                    MovableUtils.moveToLocationAtSafeZ(this, getManualNozzleTipChangeLocation());
                }
            }

            setNozzleTip(null);

            if (!changerEnabled) {
                waitForCompletion(CompletionType.WaitForStillstand);
                throw new ManualUnloadException(this, "Task interrupted: Please perform a manual nozzle tip "+nt.getName()+" unload from nozzle "+getName()+" now. "
                                + "You can then resume/restart the interrupted task.");
            }
        }

        // For manual nozzle tip changing, we will never get here because of the exception.
        // And it is also not possible anymore as nozzle tip unloading and loading is a single
        // exception/interruption now and the nozzle will usually not remain in the unloaded state.
        
        // May need to calibrate the "unloaded" nozzle tip stand-in i.e. the naked nozzle tip holder. 
        ReferenceNozzleTip calibrationNozzleTip = this.getCalibrationNozzleTip();
        if (calibrationNozzleTip != null && calibrationNozzleTip.getCalibration().isRecalibrateOnNozzleTipChangeNeeded(this)) {
            Logger.debug("{}.unloadNozzleTip() nozzle tip {} calibration needed", getName(), calibrationNozzleTip.getName());
            calibrationNozzleTip.getCalibration().calibrate(this);
        }
    }

    protected void assertManualChangeLocation() throws Exception {
        if (isManualNozzleTipChangeLocationUndefined()) {
            throw new Exception("Nozzle "+getName()+" Manual Change Location is not configured!");
        }
    }

    protected boolean isManualNozzleTipChangeLocationUndefined() {
        return manualNozzleTipChangeLocation.equals(new Location(LengthUnit.Millimeters));
    }

    public boolean isChangerEnabled() {
        return changerEnabled;
    }

    public void setChangerEnabled(boolean changerEnabled) {
        this.changerEnabled = changerEnabled;
    }

    protected void ensureZCalibrated(boolean assumeNozzleTipLoaded) throws Exception {}

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceNozzleConfigurationWizard(getMachine(), this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard()),
                new PropertySheetWizardAdapter(new ReferenceNozzleCompatibleNozzleTipsWizard(this),
                        Translations.getString("ReferenceNozzle.PropertySheetHolder.NozzleTips.title")), //$NON-NLS-1$
                new PropertySheetWizardAdapter(new ReferenceNozzleVacuumWizard(this),
                        Translations.getString("ReferenceNozzle.PropertySheetHolder.Vacuum.title")), //$NON-NLS-1$
                new PropertySheetWizardAdapter(new ReferenceNozzleToolChangerWizard(this),
                        Translations.getString("ReferenceNozzle.PropertySheetHolder.ToolChanger.title")), //$NON-NLS-1$
                new PropertySheetWizardAdapter(new ReferenceNozzleCameraOffsetWizard(this),
                        Translations.getString("ReferenceNozzle.PropertySheetHolder.OffsetWizard.title")), //$NON-NLS-1$
        };
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] {deleteAction};
    }

    public Action deleteAction = new AbstractAction("Delete Nozzle") {
        {
            putValue(SMALL_ICON, Icons.nozzleRemove);
            putValue(NAME, Translations.getString("ReferenceNozzle.Action.Delete")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("ReferenceNozzle.Action.Delete.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (getHead().getNozzles().size() == 1) {
                MessageBoxes.errorBox(null, "Error: Nozzle Not Deleted", "Can't delete last nozzle. There must be at least one nozzle.");
                return;
            }
            int ret = JOptionPane.showConfirmDialog(MainFrame.get(),
                    Translations.getString("DialogMessages.ConfirmDelete.text") + " " + getName() + "?", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    Translations.getString("DialogMessages.ConfirmDelete.title") + " " + getName() + "?", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                getHead().removeNozzle(ReferenceNozzle.this);
            }
        }
    };

    @Override
    public String toString() {
        return getName() + " " + getId();
    }

    protected boolean isVaccumSenseActuatorEnabled() {
        return vacuumSenseActuator != null;
    }

    protected boolean isVaccumActuatorEnabled() {
        return vacuumActuator != null;
    }

    @Override
    public boolean isPartOnEnabled(Nozzle.PartOnStep step) {
        if ((step == PartOnStep.AfterPick && getNozzleTip().isPartOnCheckAfterPick())
                || (step == PartOnStep.Align && getNozzleTip().isPartOnCheckAlign())
                || (step == PartOnStep.BeforePlace && getNozzleTip().isPartOnCheckBeforePlace())) {
            return isVaccumSenseActuatorEnabled() 
                    && (getNozzleTip().getMethodPartOn() != VacuumMeasurementMethod.None);
        }
        return false;
    }

    @Override
    public boolean isPartOffEnabled(Nozzle.PartOffStep step) {
        if ((step == PartOffStep.AfterPlace && getNozzleTip().isPartOffCheckAfterPlace())
                || (step == PartOffStep.BeforePick && getNozzleTip().isPartOffCheckBeforePick())) {
            return isVaccumSenseActuatorEnabled() 
                    && (getNozzleTip().getMethodPartOff() != VacuumMeasurementMethod.None);
        }
        return false;
    }

    /**
     * @return The actuator used to sense the vacuum on the Nozzle.
     */
    public Actuator getVacuumSenseActuator() {
        return vacuumSenseActuator;
    }

    /**
     * @return The actuator used to sense the vacuum on the Nozzle.
     */
    public Actuator getExpectedVacuumSenseActuator() throws Exception {
        Actuator actuator = getVacuumSenseActuator();
        if (actuator == null) {
            throw new Exception("Nozzle "+getName()+" has no vacuum sense actuator assigned.");
        }
        return actuator;
    }

    /**
     * Set the actuator used to sense the vacuum on the Nozzle.
     * @param actuator
     */
    public void setVacuumSenseActuator(Actuator actuator) {
        vacuumSenseActuator = actuator;
    }

    /**
     * @return The actuator used to switch the vacuum valve on the Nozzle. 
     */
    public Actuator getVacuumActuator() {
        return vacuumActuator;
    }

    /**
     * @return The actuator used to switch the vacuum valve on the Nozzle. 
     * @throws Exception when the actuator is not configured.
     */
    public Actuator getExpectedVacuumActuator() throws Exception {
        Actuator actuator = getVacuumActuator();
        if (actuator == null) {
            throw new Exception("Nozzle "+getName()+" has no vacuum actuator assigned.");
        }
        return actuator;
    }

    /**
     * Set the actuator used to switch the vacuum valve on the Nozzle. 
     * @param actuator
     */
    public void setVacuumActuator(Actuator actuator) {
        vacuumActuator = actuator;
    }

    /**
     * @return The actuator used to blow off parts on the Nozzle. 
     */
    public Actuator getBlowOffActuator() {
        return blowOffActuator;
    }

    /**
     * @return The actuator used to blow off parts on the Nozzle. 
     * @throws Exception when the actuator is not configured.
     */
    public Actuator getExpectedBlowOffActuator() throws Exception {
        Actuator actuator = getBlowOffActuator();
        if (actuator == null) {
            throw new Exception("Nozzle "+getName()+" has no blow off actuator assigned.");
        }
        return actuator;
    }

    /**
     * Set the actuator used to blow off parts on the Nozzle. 
     * @param actuator
     */
    public void setBlowOffActuator(Actuator actuator) {
        blowOffActuator = actuator;
    }

    public boolean isBlowOffClosingValve() {
        return blowOffClosingValve;
    }

    public void setBlowOffClosingValve(boolean blowOffClosingValve) {
        this.blowOffClosingValve = blowOffClosingValve;
    }

    protected void actuateVacuumValve(boolean on) throws Exception {
        if (on) {
            getHead().actuatePumpRequest(this, true);
        }

        getExpectedVacuumActuator().actuate(on);

        if (! on) {
            getHead().actuatePumpRequest(this, false);
        }
    }

    protected void actuateVacuumValve(double value) throws Exception {
        getHead().actuatePumpRequest(this, true);

        getExpectedVacuumActuator().actuate(value);
    }

    protected void actuateBlowValve(double value) throws Exception {
        getExpectedBlowOffActuator().actuate(value);

        getHead().actuatePumpRequest(this, false);
    }

    public double readVacuumLevel() throws Exception {
        return Double.parseDouble(getExpectedVacuumSenseActuator().read());
    }

    protected boolean isPartOnGraphEnabled() {
        ReferenceNozzleTip nt = getNozzleTip();
        return nt.getMethodPartOn() != VacuumMeasurementMethod.None
                && (nt.getMethodPartOn().isDifferenceMethod() || nt.isEstablishPartOnLevel());
    }

    protected boolean isPartOffGraphEnabled() {
        ReferenceNozzleTip nt = getNozzleTip();
        return nt.getMethodPartOff() != VacuumMeasurementMethod.None
                && (nt.getMethodPartOff().isDifferenceMethod() || nt.isEstablishPartOffLevel());
    }

    protected void storeBeforePickVacuumLevel() throws Exception {
        ReferenceNozzleTip nt = getNozzleTip();
        if (isPartOnGraphEnabled()) {
            // start a new graph 
            double vacuumLevel = readVacuumLevel();
            SimpleGraph vacuumGraph = nt.startNewVacuumGraph(vacuumLevel, true);
            // store on the nozzle tip ... to be continued
            nt.setVacuumPartOnGraph(vacuumGraph);
        }
        else {
            nt.setVacuumPartOnGraph(null);
        }
    }

    protected void storeBeforePlaceVacuumLevel() throws Exception {
        ReferenceNozzleTip nt = getNozzleTip();
        if (isPartOffGraphEnabled()) {
            // start a new graph 
            double vacuumLevel = readVacuumLevel();
            SimpleGraph vacuumGraph = nt.startNewVacuumGraph(vacuumLevel, false);
            // store on the nozzle tip ... to be continued
            nt.setVacuumPartOffGraph(vacuumGraph);
        }
        else {
            nt.setVacuumPartOffGraph(null);
        }
    }

    protected void establishPickVacuumLevel(int milliseconds) throws Exception {
        ReferenceNozzleTip nt = getNozzleTip();
        SimpleGraph vacuumGraph = nt.getVacuumPartOnGraph();
        if (vacuumGraph != null) {
            // valve is sure on
            vacuumGraph.getRow(ReferenceNozzleTip.BOOLEAN, ReferenceNozzleTip.VALVE_ON)
            .recordDataPoint(vacuumGraph.getT(), 1);
            long timeout = System.currentTimeMillis() + milliseconds;
            SimpleGraph.DataRow vacuumData = vacuumGraph.getRow(ReferenceNozzleTip.PRESSURE, ReferenceNozzleTip.VACUUM);
            double vacuumLevel;
            do {
                vacuumLevel = readVacuumLevel();
                vacuumData.recordDataPoint(vacuumGraph.getT(), vacuumLevel);
                if (nt.isEstablishPartOnLevel() 
                        && vacuumLevel >= nt.getVacuumLevelPartOnLow() && vacuumLevel <= nt.getVacuumLevelPartOnHigh()) {
                    // within range, we're done
                    break;
                }
            }
            while (System.currentTimeMillis() < timeout);
            // valve is still on
            vacuumGraph.getRow(ReferenceNozzleTip.BOOLEAN, ReferenceNozzleTip.VALVE_ON)
                .recordDataPoint(vacuumGraph.getT(), 1);
            nt.setVacuumPartOnGraph(vacuumGraph);
            if (nt.getMethodPartOn().isDifferenceMethod()) {
                nt.setVacuumLevelPartOnReading(vacuumLevel);
            }
        }
        else {
            // simple method, just dwell
            // if dwelling is handled via Thread.sleep() full machine coordination with wait for stillstand is required
            waitForCompletion(CompletionType.WaitForStillstand);
            Logger.trace(getName()+" dwell for pick vacuum "+milliseconds+"ms");
            delay(milliseconds);
        }
    }

    protected void establishPlaceVacuumLevel(int milliseconds) throws Exception {
        ReferenceNozzleTip nt = getNozzleTip();
        SimpleGraph vacuumGraph = nt.getVacuumPartOffGraph();
        if (vacuumGraph != null) {
            // valve is sure off
            vacuumGraph.getRow(ReferenceNozzleTip.BOOLEAN, ReferenceNozzleTip.VALVE_ON)
            .recordDataPoint(vacuumGraph.getT(), 0);
            long timeout = System.currentTimeMillis() + milliseconds;
            SimpleGraph.DataRow vacuumData = vacuumGraph.getRow(ReferenceNozzleTip.PRESSURE, ReferenceNozzleTip.VACUUM);
            double vacuumLevel;
            do {
                vacuumLevel = readVacuumLevel();
                vacuumData.recordDataPoint(vacuumGraph.getT(), vacuumLevel);
                if (nt.isEstablishPartOffLevel() 
                        && vacuumLevel >= nt.getVacuumLevelPartOffLow() && vacuumLevel <= nt.getVacuumLevelPartOffHigh()) {
                    // within range, we're done
                    break;
                }
            }
            while (System.currentTimeMillis() < timeout);
            // valve is still off
            vacuumGraph.getRow(ReferenceNozzleTip.BOOLEAN, ReferenceNozzleTip.VALVE_ON)
                .recordDataPoint(vacuumGraph.getT(), 0);
            nt.setVacuumPartOffGraph(vacuumGraph);
            if (nt.getMethodPartOff().isDifferenceMethod()) {
                nt.setVacuumLevelPartOffReading(vacuumLevel);
            }
        }
        else {
            // simple method, just dwell
            // if dwelling is handled via Thread.sleep() full machine coordination with wait for stillstand is required
            waitForCompletion(CompletionType.WaitForStillstand);
            Logger.trace(getName()+" dwell for place vacuum dissipation "+milliseconds+"ms");
            delay(milliseconds);
        }
    }

    protected double probePartOffVacuumLevel(int probingMilliseconds, int dwellMilliseconds) throws Exception {
        ReferenceNozzleTip nt = getNozzleTip();
        SimpleGraph vacuumGraph = null;
        double returnedVacuumLevel = Double.NaN; // this should always be overwritten in one or the other if/else combo 
        if (isPartOnGraphEnabled()) {
            vacuumGraph = nt.getVacuumPartOffGraph();
            if (vacuumGraph == null || vacuumGraph.getT() > 1000.0) {
                // Time since last action too long, this is probably a BeforePick check, start a new graph.
                vacuumGraph = nt.startNewVacuumGraph(readVacuumLevel(), true);
                nt.setVacuumPartOffGraph(vacuumGraph);
            }
            // record valve off
            vacuumGraph.getRow(ReferenceNozzleTip.BOOLEAN, ReferenceNozzleTip.VALVE_ON)
            .recordDataPoint(vacuumGraph.getT(), 0);
        }

        if (nt.getMethodPartOff().isDifferenceMethod()) {
            // we might have multiple partOff checks, so refresh the difference baseline
            double vacuumLevel = readVacuumLevel();
            // store in graph, if one is present
            if (vacuumGraph != null) {
                vacuumGraph.getRow(ReferenceNozzleTip.PRESSURE, ReferenceNozzleTip.VACUUM)
                .recordDataPoint(vacuumGraph.getT(), vacuumLevel);
            }
            // store as baseline
            nt.setVacuumLevelPartOffReading(vacuumLevel);
        }

        try {
            // switch vacuum on for the test
            actuateVacuumValve(true);

            if (vacuumGraph != null) {
                // record valve on
                vacuumGraph.getRow(ReferenceNozzleTip.BOOLEAN, ReferenceNozzleTip.VALVE_ON)
                .recordDataPoint(vacuumGraph.getT(), 1);
                // record the slope of the vacuum level
                long timeout = System.currentTimeMillis() + probingMilliseconds;
                SimpleGraph.DataRow vacuumData = vacuumGraph.getRow(ReferenceNozzleTip.PRESSURE, ReferenceNozzleTip.VACUUM);
                double vacuumLevel;
                do {
                    vacuumLevel = readVacuumLevel();
                    vacuumData.recordDataPoint(vacuumGraph.getT(), vacuumLevel);
                }
                while (System.currentTimeMillis() < timeout);
                // record valve still on 
                vacuumGraph.getRow(ReferenceNozzleTip.BOOLEAN, ReferenceNozzleTip.VALVE_ON)
                .recordDataPoint(vacuumGraph.getT(), 1);
                if (dwellMilliseconds <= 0) {
                    returnedVacuumLevel = vacuumLevel;
                }
            }
            else {
                // simple method, just dwell
                // if dwelling is handled via Thread.sleep() full machine coordination with wait for stillstand is required
                waitForCompletion(CompletionType.WaitForStillstand);
                Logger.trace(getName()+" dwell for part off probing, open valve "+probingMilliseconds+"ms");
                delay(probingMilliseconds);
                if (dwellMilliseconds <= 0) {
                    returnedVacuumLevel = readVacuumLevel();
                }
            }
        }
        finally {
            // always make sure the valve is off
            actuateVacuumValve(false);
        }

        if (vacuumGraph != null) {
            // record valve off
            vacuumGraph.getRow(ReferenceNozzleTip.BOOLEAN, ReferenceNozzleTip.VALVE_ON)
            .recordDataPoint(vacuumGraph.getT(), 0);
            // record the slope of the vacuum level
            long timeout = System.currentTimeMillis() + dwellMilliseconds;
            SimpleGraph.DataRow vacuumData = vacuumGraph.getRow(ReferenceNozzleTip.PRESSURE, ReferenceNozzleTip.VACUUM);
            double vacuumLevel;
            do {
                vacuumLevel = readVacuumLevel();
                vacuumData.recordDataPoint(vacuumGraph.getT(), vacuumLevel);
            }
            while (System.currentTimeMillis() < timeout);
            // record valve still off
            vacuumGraph.getRow(ReferenceNozzleTip.BOOLEAN, ReferenceNozzleTip.VALVE_ON)
            .recordDataPoint(vacuumGraph.getT(), 0);
            // save the graph back (for the property change to fire)
            nt.setVacuumPartOffGraph(vacuumGraph);
            if (dwellMilliseconds > 0) {
                returnedVacuumLevel = vacuumLevel;
            }
            // return the vacuum level, either from before or after valve closed
            return returnedVacuumLevel;
        }
        else {
            // simple method, just dwell and then read the level
            if (dwellMilliseconds > 0) {
                // if dwelling is handled via Thread.sleep() full machine coordination with wait for stillstand is required
                waitForCompletion(CompletionType.WaitForStillstand);
                Logger.trace(getName()+" dwell for part off probing, closed valve "+dwellMilliseconds+"ms");
                delay(dwellMilliseconds);
                returnedVacuumLevel = readVacuumLevel();
            }
            // return the vacuum level, either from before or after valve closed
            return returnedVacuumLevel;
        }
    }

    /**
     * Delay for a given time in milliseconds and take the respective vacuum valve into account.
     * @param milliseconds
     * @throws Exception 
     */
    private void delay(int milliseconds) throws Exception  {
        delay(milliseconds, getExpectedVacuumActuator());
    }

    @Override
    public boolean isPartOn() throws Exception {
        ReferenceNozzleTip nt = getNozzleTip();
        double vacuumLevel = readVacuumLevel();
        // store in graph, if one is present
        SimpleGraph vacuumGraph = nt.getVacuumPartOnGraph();
        if (vacuumGraph != null) {
            vacuumGraph.getRow(ReferenceNozzleTip.PRESSURE, ReferenceNozzleTip.VACUUM)
                .recordDataPoint(vacuumGraph.getT(), vacuumLevel);
            // valve is still on
            vacuumGraph.getRow(ReferenceNozzleTip.BOOLEAN, ReferenceNozzleTip.VALVE_ON)
                .recordDataPoint(vacuumGraph.getT(), 1);
        }
        if (nt.getMethodPartOn().isDifferenceMethod()) {
            // observe the trend as a difference from the baseline reading
            double vacuumBaselineLevel = nt.getVacuumLevelPartOnReading();
            double vacuumDifference = vacuumLevel - vacuumBaselineLevel;
            nt.setVacuumDifferencePartOnReading(vacuumDifference);
            // check the reference range 
            if (vacuumBaselineLevel < nt.getVacuumLevelPartOnLow() || vacuumBaselineLevel > nt.getVacuumLevelPartOnHigh()) {
                Logger.debug("Nozzle tip {} baseline vacuum level {} outside PartOn range {} .. {}", 
                        nt.getName(), vacuumBaselineLevel, nt.getVacuumLevelPartOnLow(), nt.getVacuumLevelPartOnHigh());
                return false;
            }
            // so far so good, check the difference
            if (vacuumDifference < nt.getVacuumDifferencePartOnLow() || vacuumDifference > nt.getVacuumDifferencePartOnHigh()) {
                Logger.debug("Nozzle tip {} vacuum level difference {} outside PartOn range {} .. {}", 
                        nt.getName(), vacuumDifference, nt.getVacuumDifferencePartOnLow(), nt.getVacuumDifferencePartOnHigh());
                return false;
            }
        }
        else {
            // absolute method, store this as last level reading
            nt.setVacuumLevelPartOnReading(vacuumLevel);
            // no trend
            nt.setVacuumDifferencePartOnReading(null);
            // check the range
            if (vacuumLevel < nt.getVacuumLevelPartOnLow() || vacuumLevel > nt.getVacuumLevelPartOnHigh()) {
                Logger.debug("Nozzle tip {} absolute vacuum level {} outside PartOn range {} .. {}", 
                        nt.getName(), vacuumLevel, nt.getVacuumLevelPartOnLow(), nt.getVacuumLevelPartOnHigh());
                return false;
            }
        }
        // success
        return true;
    }

    @Override
    public boolean isPartOff() throws Exception {
        ReferenceNozzleTip nt = getNozzleTip();
        // perform the probing pulse and decay dwell, get the resulting vacuum level 
        double vacuumLevel = probePartOffVacuumLevel(nt.getPartOffProbingMilliseconds(), nt.getPartOffDwellMilliseconds());

        if (nt.getMethodPartOff().isDifferenceMethod()) {
            // observe the trend as a difference from the baseline reading
            double vacuumBaselineLevel = nt.getVacuumLevelPartOffReading();
            double vacuumDifference = vacuumLevel - vacuumBaselineLevel;
            nt.setVacuumDifferencePartOffReading(vacuumDifference);
            // check the reference range 
            if (vacuumBaselineLevel < nt.getVacuumLevelPartOffLow() || vacuumBaselineLevel > nt.getVacuumLevelPartOffHigh()) {
                Logger.debug("Nozzle tip {} baseline vacuum level {} outside PartOff range {} .. {}", 
                        nt.getName(), vacuumBaselineLevel, nt.getVacuumLevelPartOffLow(), nt.getVacuumLevelPartOffHigh());
                return false;
            }
            // so far so good, check the difference
            if (vacuumDifference < nt.getVacuumDifferencePartOffLow() || vacuumDifference > nt.getVacuumDifferencePartOffHigh()) {
                Logger.debug("Nozzle tip {} vacuum level difference {} outside PartOff range {} .. {}", 
                        nt.getName(), vacuumDifference, nt.getVacuumDifferencePartOffLow(), nt.getVacuumDifferencePartOffHigh());
                return false;
            }
        }
        else {
            // absolute method, store this as last level reading
            nt.setVacuumLevelPartOffReading(vacuumLevel);
            // no trend
            nt.setVacuumDifferencePartOffReading(null);
            // check the range
            if (vacuumLevel < nt.getVacuumLevelPartOffLow() || vacuumLevel > nt.getVacuumLevelPartOffHigh()) {
                Logger.debug("Nozzle tip {} absolute vacuum level {} outside PartOff range {} .. {}", 
                        nt.getName(), vacuumLevel, nt.getVacuumLevelPartOffLow(), nt.getVacuumLevelPartOffHigh());
                return false;
            }
        }
        // success
        return true;
    }

    @Override
    public void findIssues(Solutions solutions) {
        super.findIssues(solutions);
        try {
            if (solutions.isTargeting(Milestone.Basics)) {
                ActuatorSolutions.findActuateIssues(solutions, this, getVacuumActuator(), "vacuum valve",
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Vacuum-Setup");
                if (getBlowOffActuator() != null) {
                    AbstractActuator.suggestValueType(getBlowOffActuator(), ActuatorValueType.Double);
                    ActuatorSolutions.findActuateIssues(solutions, this, getBlowOffActuator(), "blow off",
                            "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Vacuum-Setup");
                }
                // If at least one nozzle tip uses vacuum sensing, require a sensing actuator.
                boolean needsSensing = false;
                for (NozzleTip tip : Configuration.get().getMachine().getNozzleTips()) {
                    if (tip instanceof ReferenceNozzleTip) {
                        ReferenceNozzleTip referenceNozzleTip = (ReferenceNozzleTip) tip;
                        if (referenceNozzleTip.getMethodPartOn() != VacuumMeasurementMethod.None
                                || referenceNozzleTip.getMethodPartOff() != VacuumMeasurementMethod.None) {
                            needsSensing = true;
                            break;
                        }
                    }
                }
                if (needsSensing) {
                    ActuatorSolutions.findActuatorReadIssues(solutions, this, getVacuumSenseActuator(), "vacuum sensing", 
                            "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Vacuum-Sensing#actuator-setup");
                }
            }
        }
        catch (Exception e) {
            Logger.warn(e);
        }
        ContactProbeNozzle.addConversionIssue(solutions, this);
    }

    @Deprecated
    public void migrateSafeZ() {
        if (safeZ == null) {
            safeZ = new Length(0, LengthUnit.Millimeters);
        }
        CoordinateAxis coordAxis = getCoordinateAxisZ();
        if (coordAxis instanceof ReferenceControllerAxis) {
            ReferenceControllerAxis rawAxis = (ReferenceControllerAxis) coordAxis; 
            try {
                Length rawZ = headMountableToRawZ(rawAxis, safeZ);
                rawAxis.setSafeZoneLow(rawZ);
                rawAxis.setSafeZoneLowEnabled(true);
                rawAxis.setSafeZoneHigh(rawZ);
                rawAxis.setSafeZoneHighEnabled(true);
                // Get rid of the old setting.
                safeZ = null;
            }
            catch (Exception e) {
                Logger.error(e);
            }
        }
        else if (coordAxis != null) {
            coordAxis.setHomeCoordinate(safeZ);
        }
    }
}
