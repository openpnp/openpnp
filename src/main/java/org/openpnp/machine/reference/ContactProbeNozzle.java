/*
 * Copyright (C) 2019 <mark@makr.zone>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.reference.ReferenceNozzleTip.VacuumMeasurementMethod;
import org.openpnp.machine.reference.ReferenceNozzleTip.ZCalibrationTrigger;
import org.openpnp.machine.reference.driver.GcodeAsyncDriver;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.machine.reference.driver.GcodeDriverSolutions;
import org.openpnp.machine.reference.wizards.ContactProbeNozzleWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Issue;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.spi.base.AbstractHead;
import org.openpnp.spi.base.AbstractPnpJobProcessor;
import org.openpnp.util.Collect;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;
import org.simpleframework.xml.stream.Style;

public class ContactProbeNozzle extends ReferenceNozzle {

    public ContactProbeNozzle() {
        super();
    }

    public ContactProbeNozzle(String id) {
        super(id);
    }

    @Element(required = false)
    private String contactProbeActuatorName = "";
    private boolean isDisabled;

    @Element(required = false)
    private Length contactProbeStartOffsetZ = new Length(1, LengthUnit.Millimeters);

    @Element(required = false)
    private Length contactProbeDepthZ = new Length(2, LengthUnit.Millimeters);

    @Element(required = false)
    private Length sniffleIncrementZ = new Length(0.1, LengthUnit.Millimeters); 

    @Attribute(required=false)
    private long sniffleDwellTime = 250;

    @Element(required = false)
    private Length contactProbeAdjustZ = new Length(0, LengthUnit.Millimeters);

    public enum ContactProbeMethod {
        None,
        VacuumSense,
        ContactSenseActuator;

        public boolean isPlacementCompatible() {
            return this == ContactSenseActuator;
        }
    };
    @Attribute(required=false)
    private ContactProbeMethod contactProbeMethod = ContactProbeMethod.ContactSenseActuator;

    public enum ContactProbeTrigger {
        Off,
        Once,
        AfterHoming,
        EachTime
    };

    @Attribute(required=false)
    private ContactProbeTrigger feederHeightProbing = ContactProbeTrigger.EachTime;

    @Attribute(required=false)
    private ContactProbeTrigger partHeightProbing = ContactProbeTrigger.EachTime;

    @Element(required=false)
    private Length calibrationOffsetZ = null;

    @Attribute(required=false)
    private double maxZOffsetMm = 2.0;

    @ElementMap(required = false)
    private HashMap<Feeder, Length> probedFeederHeightOffsets = new HashMap<>();

    @ElementMap(required = false)
    private HashMap<Part, Length> probedPartHeightOffsets = new HashMap<>();

    private ReferenceNozzleTip zCalibratedNozzleTip;

    @Override
    public PropertySheet[] getPropertySheets() {
        return Collect.concat(super.getPropertySheets(), new PropertySheet[] {
                new PropertySheetWizardAdapter(new ContactProbeNozzleWizard(this), "Contact Probe") });
    }

    @Override
    public void home() throws Exception {
        if (feederHeightProbing == ContactProbeTrigger.Off
                || feederHeightProbing == ContactProbeTrigger.AfterHoming
                || contactProbeMethod != ContactProbeMethod.ContactSenseActuator) {
            // Reset the probed feeder heights.
            probedFeederHeightOffsets.clear();
        }
        if (partHeightProbing == ContactProbeTrigger.Off
                || partHeightProbing == ContactProbeTrigger.AfterHoming
                || contactProbeMethod != ContactProbeMethod.ContactSenseActuator) {
            // Reset the probed part heights.
            probedPartHeightOffsets.clear();
        }
        ReferenceNozzleTip nt = getCalibrationNozzleTip();
        if (nt != null && nt.getzCalibrationTrigger() != ZCalibrationTrigger.Manual) {
            // After homing, recalibrate.
            try {
                zCalibratedNozzleTip = null;
                ensureZCalibrated();
            }
            catch (Exception e) {
                if (nt != null && nt.iszCalibrationFailHoming()) {
                    throw e;
                }
                else {
                    UiUtils.messageBoxOnExceptionLater(() -> {
                        throw e;
                    });
                }
            }
        }
        super.home();
    }

    protected boolean isFeederHeightProbingNeeded(Feeder feeder) {
        if (isDisabled || feeder == null) {
            return false;
        }
        if (contactProbeMethod != ContactProbeMethod.ContactSenseActuator
                || feederHeightProbing == ContactProbeTrigger.Off) {
            return false;
        }
        if (feederHeightProbing == ContactProbeTrigger.EachTime) {
            return true;
        }
        if (feeder.isPartHeightAbovePickLocation()
                && feeder.getPart().isPartHeightUnknown()) {
            return true;
        }
        Length z = probedFeederHeightOffsets.get(feeder);
        return (z == null);
    }

    protected boolean isPartHeightProbingNeeded(Part part) {
        if (isDisabled || part == null) {
            return false;
        }
        if (contactProbeMethod != ContactProbeMethod.ContactSenseActuator
                || partHeightProbing == ContactProbeTrigger.Off) {
            return false;
        }
        if (feederHeightProbing == ContactProbeTrigger.EachTime) {
            return true;
        }
        if (part.isPartHeightUnknown()) {
            return true;
        }
        Length z = probedPartHeightOffsets.get(part);
        return (z == null);
    }

    @Override
    public void moveToPickLocation(Feeder feeder) throws Exception {
        Part part = feeder.getPart();
        Location pickLocation = feeder.getPickLocation();
        Location pickLocationPart = pickLocation;
        boolean partHeightProbing = false;
        if (feeder.isPartHeightAbovePickLocation()) {
            partHeightProbing = part.isPartHeightUnknown();
            Length partHeight = getSafePartHeight(part);
            pickLocationPart = pickLocationPart.add(new Location(partHeight.getUnits(), 0, 0, partHeight.getValue(), 0));
        }

        if (isFeederHeightProbingNeeded(feeder)) {
            moveAboveProbingLocation(pickLocationPart);

            try {
                Map<String, Object> globals = new HashMap<>();
                globals.put("nozzle", this);
                globals.put("feeder", feeder);
                globals.put("part", part);
                Configuration.get().getScripting().on("Nozzle.BeforePickProbe", globals);
            } catch (Exception e) {
                Logger.warn(e);
            }

            Length depthZ;
            if (partHeightProbing) {
                // We're probing for the part height.
                depthZ = nozzleTip.getMaxPartHeight();
            }
            else {
                depthZ = contactProbeDepthZ;
            }
            // Probe down from current position above the part until the probe sensor
            // is triggered.
            Location probedLocation = contactProbe(true, depthZ);
            Length offsetZ;
            if (partHeightProbing) { 
                // Part height is difference to pick location (as part is above it). 
                Length partHeight = probedLocation.subtract(pickLocation).getLengthZ();
                part.setHeight(partHeight);
                offsetZ = new Length(0, LengthUnit.Millimeters);
                Logger.info("Nozzle "+getName()+" probed part "+part.getId()+" height at "+partHeight);
            }
            else {
                offsetZ = probedLocation.subtract(pickLocationPart).getLengthZ();
                Logger.debug("Nozzle "+getName()+" probed feeder "+feeder.getName()+" Z at offset "+offsetZ);
            }
            // Store the probed location offset.
            probedFeederHeightOffsets.put(feeder, offsetZ);

            // Retract from probing e.g. until the probe sensor is released.
            contactProbe(false, contactProbeDepthZ);

            try {
                Map<String, Object> globals = new HashMap<>();
                globals.put("nozzle", this);
                globals.put("feeder", feeder);
                globals.put("part", part);
                Configuration.get().getScripting().on("Nozzle.AfterPickProbe", globals);
            } catch (Exception e) {
                Logger.warn(e);
            }
        }
        else {
            Length offsetZ = probedFeederHeightOffsets.get(feeder);
            if (offsetZ != null) {
                // Apply the probed offset.
                Logger.trace("Nozzle "+getName()+" applies feeder "+feeder.getName()+" height offset "+offsetZ);
                pickLocationPart = pickLocationPart.add(new Location(offsetZ .getUnits(), 0, 0, offsetZ.getValue(), 0));
                MovableUtils.moveToLocationAtSafeZ(this, pickLocationPart);
            }
            else {
                super.moveToPickLocation(feeder);
            }
        }
    }

    @Override
    public void moveToPlacementLocation(Location placementLocation, Part part) throws Exception {
        Length partHeight = getSafePartHeight(part);
        Location placementLocationPart = placementLocation.add(new Location(partHeight.getUnits(), 0, 0, partHeight.getValue(), 0));
        if (isPartHeightProbingNeeded(part)) {
            boolean partHeightProbing = part.isPartHeightUnknown();
            // Calculate the probe starting location.
            moveAboveProbingLocation(placementLocationPart);

            try {
                Map<String, Object> globals = new HashMap<>();
                globals.put("nozzle", this);
                globals.put("part", getPart());
                Configuration.get().getScripting().on("Nozzle.BeforePlaceProbe", globals);
            } catch (Exception e) {
                Logger.warn(e);
            }

            Length depthZ;
            if (partHeightProbing) {
                // We're probing for the part height.
                depthZ = nozzleTip.getMaxPartHeight();
            }
            else {
                depthZ = contactProbeDepthZ;
            }
            // Probe down from current position above the part until the probe sensor
            // is triggered.
            Location probedLocation = contactProbe(true, depthZ);
            Length offsetZ;
            if (partHeightProbing) { 
                // Part height is difference to placement location (part is above it). 
                partHeight = probedLocation.subtract(placementLocation).getLengthZ();
                Logger.info("Nozzle "+getName()+" probed part "+part.getId()+" height at "+partHeight);
                if (partHeight.getValue() <= 0) {
                    throw new Exception("Part height "+part.getId()+" probing by nozzle "+getName()+" failed (returned negative height). Check PCB Z and probing adjustment.");
                }
                part.setHeight(partHeight);
                offsetZ = new Length(0, LengthUnit.Millimeters);
            }
            else {
                offsetZ = probedLocation.subtract(placementLocationPart).getLengthZ();
                Logger.debug("Nozzle "+getName()+" probed part "+part.getId()+" height at offset "+offsetZ);
            }
            // Store the probed location offset.
            probedPartHeightOffsets.put(part, offsetZ);

            contactProbe(false, contactProbeDepthZ);

            try {
                Map<String, Object> globals = new HashMap<>();
                globals.put("nozzle", this);
                globals.put("part", getPart());
                Configuration.get().getScripting().on("Nozzle.AfterPlaceProbe", globals);
            } catch (Exception e) {
                Logger.warn(e);
            }
        }
        else {
            Length offsetZ = probedPartHeightOffsets.get(part);
            if (offsetZ != null) {
                // Apply the probed offset.
                Logger.trace("Nozzle "+getName()+" applies part "+part.getId()+" height offset "+offsetZ);
                placementLocationPart = placementLocationPart.add(new Location(offsetZ .getUnits(), 0, 0, offsetZ.getValue(), 0));
                MovableUtils.moveToLocationAtSafeZ(this, placementLocationPart);
            }
            else {
                super.moveToPlacementLocation(placementLocationPart, part);
            }
        }
    }

    protected Actuator getContactProbeActuator() throws Exception {
        Actuator actuator = getHead().getActuatorByName(contactProbeActuatorName);
        if (actuator == null) {
            throw new Exception(String.format("Can't find contact probe actuator %s", contactProbeActuatorName));
        }
        return actuator;
    }

    public void disableContactProbeActuator(boolean set) {
        isDisabled = set;
    }

    public String getContactProbeActuatorName() {
        return contactProbeActuatorName;
    }

    public void setContactProbeActuatorName(String contactProbeActuatorName) {
        String oldValue = this.contactProbeActuatorName;
        this.contactProbeActuatorName = contactProbeActuatorName;
        if (oldValue != contactProbeActuatorName) {
            firePropertyChange("contactProbeActuatorName", oldValue, contactProbeActuatorName);
        }
    }

    public Length getContactProbeStartOffsetZ() {
        return contactProbeStartOffsetZ;
    }

    public void setContactProbeStartOffsetZ(Length contactProbeStartOffsetZ) {
        this.contactProbeStartOffsetZ = contactProbeStartOffsetZ;
    }

    public Length getContactProbeDepthZ() {
        return contactProbeDepthZ;
    }

    public void setContactProbeDepthZ(Length contactProbeDepthZ) {
        this.contactProbeDepthZ = contactProbeDepthZ;
    }

    public Length getSniffleIncrementZ() {
        return sniffleIncrementZ;
    }

    public void setSniffleIncrementZ(Length sniffleIncrementZ) {
        this.sniffleIncrementZ = sniffleIncrementZ;
    }

    public long getSniffleDwellTime() {
        return sniffleDwellTime;
    }

    public void setSniffleDwellTime(long sniffleDwellTime) {
        this.sniffleDwellTime = sniffleDwellTime;
    }

    public Length getContactProbeAdjustZ() {
        return contactProbeAdjustZ;
    }

    public void setContactProbeAdjustZ(Length contactProbeAdjustZ) {
        this.contactProbeAdjustZ = contactProbeAdjustZ;
    }

    public ContactProbeMethod getContactProbeMethod() {
        return contactProbeMethod;
    }

    public void setContactProbeMethod(ContactProbeMethod contactProbeMethod) {
        this.contactProbeMethod = contactProbeMethod;
    }

    public ContactProbeTrigger getFeederHeightProbing() {
        return feederHeightProbing;
    }

    public void setFeederHeightProbing(ContactProbeTrigger feederHeightProbing) {
        this.feederHeightProbing = feederHeightProbing;
    }

    public ContactProbeTrigger getPartHeightProbing() {
        return partHeightProbing;
    }

    public void setPartHeightProbing(ContactProbeTrigger partHeightProbing) {
        this.partHeightProbing = partHeightProbing;
    }

    /**
     * Move above the probing location at safe Z, apply the start offset as needed.
     *  
     * @param nominalLocation
     * @throws Exception
     */
    public void moveAboveProbingLocation(Location nominalLocation) throws Exception {
        nominalLocation = nominalLocation.add(new Location(contactProbeStartOffsetZ .getUnits(), 0, 0, contactProbeStartOffsetZ.getValue(), 0));
        MovableUtils.moveToLocationAtSafeZ(this, nominalLocation);
    }

    /**
     * Z-Probe this Nozzle for contact underneath the current location. When forward is true, the nozzle probes 
     * for contact and stops when contact is made. The current machine position determines the effective contact 
     * location. When forward is false, the nozzle retracts from probing (if needed).  
     *   
     * @param forward
     *  
     * @return The probed location.
     * @throws Exception
     */
    public Location contactProbe(boolean forward, Length contactProbeDepthZ) throws Exception {
        switch (contactProbeMethod) {
            case VacuumSense: {
                if (! forward) {
                    return getLocation();
                }
                // Note, we simply use the "part-off" check for sniffle probing, so all the various settings and methods can be used.
                ReferenceNozzleTip nozzleTip = getNozzleTip();
                if (nozzleTip == null) {
                    throw new Exception("Nozzle "+getName()+" cannot sniffle-probe without nozzle tip.");
                }
                if (nozzleTip.getMethodPartOff() == VacuumMeasurementMethod.None) {
                    throw new Exception("Nozzle tip "+nozzleTip.getName()+" cannot sniffle-probe without Part-Off sensing method.");
                }
                if (!isVaccumActuatorEnabled()) {
                    throw new Exception("Nozzle "+getName()+" cannot sniffle-probe without vacuum valve actuator.");
                }
                if (getVacuumSenseActuator() == null) {
                    throw new Exception("Nozzle "+getName()+" cannot sniffle-probe without vacuum sensing actuator.");
                }
                if (getPart() != null) {
                    throw new Exception("Nozzle "+getName()+" cannot sniffle-probe with part on nozzle. Free nozzle vacuum sensing needed.");
                }
                AbstractHead head = (AbstractHead) getHead();
                Location probeIncrement = new Location(sniffleIncrementZ.getUnits(), 
                        0, 0, sniffleIncrementZ.convertToUnits(LengthUnit.Millimeters).getValue(), 0);

                // Establish initially free nozzle.
                if (!isPartOff()) {
                    throw new Exception("Nozzle "+getName()+" first sniffle-probe was already sensing contact. Check the settings."); 
                }
                // We allow two times the offset, i.e. it is a +/- range we probe.   
                int count = (int) Math.ceil(contactProbeDepthZ.divide(sniffleIncrementZ));
                Location probedLocation = getLocation();
                for (int i = 0; i < count; i++) {
                    probedLocation = probedLocation.subtract(probeIncrement);
                    moveTo(probedLocation);
                    Thread.sleep(sniffleDwellTime);
                    if (! isPartOff()) {
                        // We got contact.
                        probedLocation = probedLocation.add(new Location(contactProbeAdjustZ .getUnits(), 0, 0, contactProbeAdjustZ.getValue(), 0));
                        moveTo(probedLocation);
                        return probedLocation;
                    }
                }
                throw new Exception("Nozzle "+getName()+" sniffle-probing made no contact. Check the settings."); 
            }

            case ContactSenseActuator: {
                Actuator contactProbeActuator = getContactProbeActuator();
                contactProbeActuator.actuate(forward);
                Location probedLocation = getLocation();
                if (forward) {
                    probedLocation = probedLocation.add(new Location(contactProbeAdjustZ .getUnits(), 0, 0, contactProbeAdjustZ.getValue(), 0));
                    moveTo(probedLocation);
                }
                return probedLocation;
            }

            default:
                throw new Exception("Nozzle "+getName()+" has contact probing disabled."); 
        }
    }

    /**
     * Performs a whole probing cycle: Moves above the nominalLocation, probes and retracts. 
     * 
     * @param nominalLocation
     * @return
     * @throws Exception
     */
    public Location contactProbeCycle(Location nominalLocation) throws Exception {
        moveAboveProbingLocation(nominalLocation);
        Location probedLocation = contactProbe(true, contactProbeDepthZ);
        contactProbe(false, contactProbeDepthZ);
        return probedLocation;
    }

    @Override
    public void ensureZCalibrated() throws Exception {
        if (contactProbeMethod == ContactProbeMethod.None) {
            return;
        }
        ReferenceNozzleTip nt = getCalibrationNozzleTip();
        if (nt == null) {
            return;
        }
        if (nt.getzCalibrationTrigger() == ZCalibrationTrigger.Manual) {
            return;
        }
        if (zCalibratedNozzleTip == nt 
                || (zCalibratedNozzleTip != null && nt.getzCalibrationTrigger() == ZCalibrationTrigger.MachineHome)) {
            // Already calibrated.
            return;
        }
        calibrateZ(nt);
    }

    @Override
    public Location toHeadLocation(Location location, Location currentLocation, LocationOption... options) {
        // Apply the Z calibration.
        // Check SuppressCompensation, in that case disable Z calibration
        if (! Arrays.asList(options).contains(LocationOption.SuppressDynamicCompensation)) {
            if (zCalibratedNozzleTip != null && calibrationOffsetZ != null) {
                location = location.subtract(new Location(calibrationOffsetZ .getUnits(), 0, 0, calibrationOffsetZ.getValue(), 0));
                Logger.trace("{}.transformToHeadLocation({}, ...) Z offset {}", getName(), location, calibrationOffsetZ);
            }
        }
        return super.toHeadLocation(location, currentLocation, options);
    }

    @Override
    public Location toHeadMountableLocation(Location location, Location currentLocation, LocationOption... options) {
        location = super.toHeadMountableLocation(location, currentLocation, options);
        // Unapply the Z calibration.
        // Check SuppressCompensation, in that case disable Z calibration.
        if (! Arrays.asList(options).contains(LocationOption.SuppressDynamicCompensation)) {
            if (zCalibratedNozzleTip != null && calibrationOffsetZ != null) {
                location = location.add(new Location(calibrationOffsetZ .getUnits(), 0, 0, calibrationOffsetZ.getValue(), 0));
            }
        }
        return location;
    }

    public Length getCalibrationOffsetZ() {
        return calibrationOffsetZ;
    }

    public void setCalibrationOffsetZ(Length calibrationOffsetZ) {
        Object oldValue = this.calibrationOffsetZ;
        this.calibrationOffsetZ = calibrationOffsetZ;
        firePropertyChange("calibrationOffsetZ", oldValue, calibrationOffsetZ);
        if (calibrationOffsetZ != oldValue) {
            NozzleTip nt = getCalibrationNozzleTip();
            if (nt instanceof ReferenceNozzleTip) {
                // Pseudo setter also fires in the name of the nozzle tip.
                ((ReferenceNozzleTip) nt).setCalibrationOffsetZ(calibrationOffsetZ);
            }
            Machine machine = Configuration.get().getMachine();
            if (machine instanceof ReferenceMachine) {
                ((ReferenceMachine)machine).fireMachineHeadActivity(getHead());
            }
        }
    }

    public void calibrateZ(ReferenceNozzleTip nt) throws Exception {
        if (nt != getCalibrationNozzleTip()) {
            throw new Exception("Nozzle "+getName()+" has not nozzle tip "+nt.getName()+" loaded.");
        }
        if (nt == null) {
            throw new Exception("Nozzle " + getName() + " has no nozzle tip loaded.");
        }
        resetZCalibration();
        Location nominalLocation = nt.getTouchLocation();
        Location probedLocation = contactProbeCycle(nominalLocation);
        Length offsetZ = nominalLocation.getLengthZ().subtract(probedLocation.getLengthZ());
        Logger.debug("Nozzle "+getName()+" nozzle tip "+nt.getName()+" Z calibration offset "+offsetZ);
        if (Math.abs(offsetZ.convertToUnits(LengthUnit.Millimeters).getValue()) > maxZOffsetMm) {
            throw new Exception("Nozzle "+getName()+" nozzle tip "+nt.getName()+" Z calibration offset "+offsetZ+" unexpectedly large. Check setup.");
        }
        // Remember which nozzle tip for trigger control.
        zCalibratedNozzleTip = nt;
        // Establish the new Z calibration.
        setCalibrationOffsetZ(offsetZ);
    }

    public void resetZCalibration() {
        setCalibrationOffsetZ(null);
        zCalibratedNozzleTip = null;
    }

    public static void referenceAllTouchLocationsZ() throws Exception {
        ReferenceNozzleTip templateNozzleTip = ReferenceNozzleTip.getTemplateNozzleTip();
        if (templateNozzleTip == null) {
            throw new Exception("No nozzle tip is marked as Template.");
        }
        // Always use the default nozzle.
        ContactProbeNozzle probeNozzle = ContactProbeNozzle.getDefaultNozzle();
        if (probeNozzle == null) {
            throw new Exception("No default ContactProbeNozzle found.");
        }
        if (probeNozzle.getNozzleTip() != templateNozzleTip) {
            probeNozzle.loadNozzleTip(templateNozzleTip);
        }
        probeNozzle.calibrateZ(templateNozzleTip);
        for (NozzleTip nt : Configuration.get().getMachine().getNozzleTips()) {
            if (nt != templateNozzleTip 
                    && nt instanceof ReferenceNozzleTip) {
                Location touchLocation = ((ReferenceNozzleTip) nt).getTouchLocation();
                if (touchLocation.getLinearDistanceTo(Location.origin) != 0
                        && !((ReferenceNozzleTip) nt).isUnloadedNozzleTipStandin()) {
                    Location probedLocation = probeNozzle.contactProbeCycle(touchLocation);

                    Logger.info("Nozzle tip "+nt.getName()+" touch location Z set to "+probedLocation.getLengthZ()+" (previously "+touchLocation.getLengthZ()+")");
                    ((ReferenceNozzleTip) nt).setTouchLocation(probedLocation);
                }
            }
        }
        probeNozzle.moveToSafeZ();
    }

    @Override
    public void findIssues(List<Solutions.Issue> issues) {
        super.findIssues(issues);
        try {
            if (getContactProbeActuator() instanceof AbstractActuator) {
                AbstractActuator contactProbeActuator = (AbstractActuator) getContactProbeActuator();
                if (!contactProbeActuator.isCoordinatedAfterActuate()) {
                    issues.add(new Solutions.Issue(
                            contactProbeActuator, 
                            "Contact probe actuator needs machine coordination after actuation.", 
                            "Enable After Actuation machine coordination.", 
                            Severity.Error,
                            "https://github.com/openpnp/openpnp/wiki/Motion-Planner#actuator-machine-coordination") {

                        @Override
                        public void setState(Solutions.State state) throws Exception {
                            if (confirmStateChange(state)) {
                                contactProbeActuator.setCoordinatedAfterActuate((state == Solutions.State.Solved));
                                super.setState(state);
                            }
                        }
                    });
                }
                if (getAxisZ() instanceof ControllerAxis) {
                    Driver driver = ((ControllerAxis) getAxisZ()).getDriver();
                    Driver oldDriver = contactProbeActuator.getDriver();
                    if (driver != null && driver != oldDriver) {
                        issues.add(new Solutions.Issue(
                                this, 
                                "Z driver "+driver.getName()+" not same as actuator "+contactProbeActuator.getName()+" driver "+
                                        (oldDriver == null ? "(unassigned)" : oldDriver.getName())+".", 
                                "Assign driver "+driver.getName()+" to actuator "+contactProbeActuator.getName()+".", 
                                Severity.Error,
                                "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Actuators#adding-actuators") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                if (confirmStateChange(state)) {
                                    contactProbeActuator.setDriver((state == Solutions.State.Solved) ?
                                            driver : oldDriver);
                                    super.setState(state);
                                }
                            }
                        });
                    }
                    if (driver instanceof GcodeAsyncDriver) {
                        if (!((GcodeAsyncDriver) driver).isReportedLocationConfirmation()) { 
                            issues.add(new Solutions.Issue(
                                    this, 
                                    "Z driver "+driver.getName()+" must use Location Confirmation for the probe actuator to work.", 
                                    "Enable Location Confirmation.", 
                                    Severity.Error,
                                    "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#advanced-settings") {

                                @Override
                                public void setState(Solutions.State state) throws Exception {
                                    if (confirmStateChange(state)) {
                                        ((GcodeAsyncDriver) driver).setReportedLocationConfirmation((state == Solutions.State.Solved));
                                        super.setState(state);
                                    }
                                }
                            });
                        }
                    }
                    else {
                        issues.add(new Solutions.PlainIssue(
                                this, 
                                "Z driver "+driver.getName()+" must support Location Confirmation for the Z probe actuator to work.", 
                                "Only the GcodeAsyncDriver currently supports it.", 
                                Severity.Error,
                                "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#advanced-settings"));
                    }
                }
                if (contactProbeActuator.getDriver() instanceof GcodeDriver) {
                    GcodeDriver gcodeDriver = (GcodeDriver) contactProbeActuator.getDriver();
                    String suggestedCommand = null; 
                    if (gcodeDriver.getFirmwareProperty("FIRMWARE_NAME", "").contains("Smoothieware")) {
                        suggestedCommand = 
                                "{True:G38.2 Z-42 F800 ; probe down max. range for contact with picked/placed part }\n" + 
                                "{True:M400            ; wait until machine has stopped }";
                    }
                    else if (gcodeDriver.getFirmwareProperty("FIRMWARE_NAME", "").contains("TinyG")) {
                        suggestedCommand = 
                                "{True:G38.2 Z-42 F800 ; probe down in absolute coordinates until zmin switch is hit}\n" + 
                                "{True:M400            ; wait for motion to stop}";
                    }
                    if (suggestedCommand != null) {
                        GcodeDriverSolutions.suggestGcodeCommand(gcodeDriver, contactProbeActuator, issues, 
                                GcodeDriver.CommandType.ACTUATE_BOOLEAN_COMMAND, suggestedCommand, false, false);
                    }
                    else {
                        issues.add(new Solutions.PlainIssue(
                                this, 
                                "Missing ACTUATE_BOOLEAN_COMMAND for actuator "+contactProbeActuator.getName()+" on driver "+gcodeDriver.getName()+" (no suggestion available for detected firmware).", 
                                "Please add the command manually.",
                                Severity.Error,
                                "https://github.com/openpnp/openpnp/wiki/Contact-Probing-Nozzle#setting-up-the-g-code"));
                    }
                }
            }
        }
        catch (Exception e) {
            // Ignore a stale or missing Actuator name here.
        }
    }

    protected boolean isPartHeightSensingAvailable(Part part, NozzleTip nozzleTip) {
        Machine machine = Configuration.get().getMachine();
        if (part != null && AbstractPnpJobProcessor.findPartAligner(machine, part) != null) {
            // Uses Alignment, so it also needs a vision based method.
            Camera camera;
            try {
                camera = VisionUtils.getBottomVisionCamera();
                return (camera.getFocusProvider() != null);
            }
            catch (Exception e) {
            }
            return false;
        }
        else {
            // It must support a placement height probing method.
            return (getContactProbeMethod()
                    .isPlacementCompatible());
        }
    }

    @Override
    protected boolean isNozzleTipAndPartCompatible(NozzleTip nt, Part part) {
        if (! super.isNozzleTipAndPartCompatible(nt, part)) {
            return false;
        }
        if (part.getHeight().getValue() <= 0) {
            // Part height unknown, part height sensing needed.
            if (! isPartHeightSensingAvailable(part, nt)) {
                return false;
            }
        }
        return true; 
    }

    public static ContactProbeNozzle getDefaultNozzle() {
        Machine machine = Configuration.get().getMachine();
        for (Head head : machine.getHeads()) {
            for (Nozzle nozzle : head.getNozzles()) {
                if (nozzle instanceof ContactProbeNozzle) {
                    if (((ContactProbeNozzle) nozzle).getContactProbeMethod() != ContactProbeMethod.None) {
                        return (ContactProbeNozzle) nozzle;
                    }
                }
            }
        }
        return null;
    }
    public static boolean isConfigured() {
        return getDefaultNozzle() != null;
    }
    public static void addConversionIssue(List<Issue> issues, final ReferenceNozzle nozzle) {
        if (! (nozzle instanceof ContactProbeNozzle)) {
            issues.add(new Solutions.Issue(
                    nozzle, 
                    "The nozzle can be replaced with a ContactProbeNozzle to support various probing features.", 
                    "Replace with ContactProbeNozzle.", 
                    Severity.Fundamental,
                    "https://github.com/openpnp/openpnp/wiki/Contact-Probing-Nozzle") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (confirmStateChange(state)) {
                        if (state == Solutions.State.Solved) {
                            ContactProbeNozzle contactProbeNozzle = convertToContactProbe(nozzle);
                            replaceNozzle(contactProbeNozzle);
                        }
                        else if (getState() == Solutions.State.Solved) {
                            // Place the old one back (from the captured ImageCamera.this).
                            replaceNozzle(nozzle);
                        }
                        super.setState(state);
                    }
                }
            });
        }
    }

    private static Serializer createSerializer() {
        Style style = new HyphenStyle();
        Format format = new Format(style);
        AnnotationStrategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy, format);
        return serializer;
    }

    /**
     * Convert an existing ReferenceNozzle to a ContactProbeNozzle while keeping all settings.
     * 
     * @param nozzle
     * @return
     * @throws Exception
     */
    public static ContactProbeNozzle convertToContactProbe(ReferenceNozzle nozzle) throws Exception {
        // Serialize the nozzle
        Serializer serOut = createSerializer();
        StringWriter sw = new StringWriter();
        serOut.write(nozzle, sw);
        String serialized = sw.toString();
        // Patch it.
        serialized.replace(
                nozzle.getClass().getCanonicalName(), 
                ContactProbeNozzle.class.getCanonicalName());
        // De-serialize it.
        Serializer serIn = createSerializer();
        StringReader sr = new StringReader(serialized);
        ContactProbeNozzle contactProbeNozzle = serIn.read(ContactProbeNozzle.class, sr);
        contactProbeNozzle.applyConfiguration(Configuration.get());
        contactProbeNozzle.setHead(nozzle.getHead());
        return contactProbeNozzle;
    }

    /**
     * Replace a nozzle with the same Id at the same place in the head nozzles list.
     * 
     * @param nozzle
     * @throws Exception
     */
    public static void replaceNozzle(Nozzle nozzle) throws Exception {
        // Find the old nozzle with the same Id.
        List<Nozzle> list = nozzle.getHead().getNozzles();
        Nozzle replaced = null;
        int index;
        for (index = 0; index < list.size(); index++) {
            if (list.get(index).getId().equals(nozzle.getId())) {
                replaced = list.get(index);
                nozzle.getHead().removeNozzle(replaced);
                break;
            }
        }
        // Add the new one.
        nozzle.getHead().addNozzle(nozzle);
        // Permutate it back to the old list place (cumbersome but works).
        for (int p = list.size()-index; p > 1; p--) {
            nozzle.getHead().permutateNozzle(nozzle, -1);
        }
    }
}