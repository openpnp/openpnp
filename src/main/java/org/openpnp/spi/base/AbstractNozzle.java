package org.openpnp.spi.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import org.openpnp.gui.support.Icons;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Camera;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

public abstract class AbstractNozzle extends AbstractHeadMountable implements Nozzle {
    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;

    @Attribute(required = false)
    protected RotationMode rotationMode = RotationMode.AbsolutePartAngle;

    @ElementList(required = false)
    protected List<String> compatibleNozzleTipIds = new ArrayList<>();

    protected Set<NozzleTip> compatibleNozzleTips; 

    protected Head head;

    protected Part part;

    protected Double rotationModeOffset;

    public AbstractNozzle() {
        this.id = Configuration.createId("NOZ");
        this.name = getClass().getSimpleName();
    }
    
    @Override
    public String getId() {
        return id;
    }

    @Override
    public Head getHead() {
        return head;
    }

    @Override
    public void setHead(Head head) {
        this.head = head;
    }

    @Override
    public Location getCameraToolCalibratedOffset(Camera camera) {
        return new Location(camera.getUnitsPerPixel().getUnits());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        firePropertyChange("name", null, name);
    }

    protected void setPart(Part part) {
        Object oldValue = this.part;
        this.part = part;
        if (rotationModeOffset != null && part == null) {
            rotationModeOffset = null;
            getMachine().fireMachineHeadActivity(head);
        }
        firePropertyChange("part", oldValue, part);
    }

    @Override
    public Part getPart() {
        return part;
    }

    @Override
    public RotationMode getRotationMode() {
        return rotationMode;
    }

    public void setRotationMode(RotationMode rotationMode) {
        this.rotationMode = rotationMode;
    }

    public double[] getRotationModeLimits() throws Exception {
        double limit0 = -180;
        double limit1 = 180;
        CoordinateAxis coordinateAxis = getAxisRotation().getCoordinateAxes(getMachine())
                .getAxis(Axis.Type.Rotation);
        if (coordinateAxis instanceof ReferenceControllerAxis) {
            ReferenceControllerAxis refAxis = (ReferenceControllerAxis) coordinateAxis;
            if (refAxis.isLimitRotation()) {
                if (refAxis.isSoftLimitLowEnabled()) {
                    AxesLocation axesLimit = getMappedAxes(getMachine())
                            .put(new AxesLocation(refAxis, refAxis.getSoftLimitLow()));
                    Location limit = toTransformed(axesLimit, LocationOption.Quiet);
                    limit = toHeadMountableLocation(limit, LocationOption.Quiet);
                    limit0 = limit.getRotation();
                }
                if (refAxis.isSoftLimitHighEnabled()) {
                    AxesLocation axesLimit = getMappedAxes(getMachine())
                            .put(new AxesLocation(refAxis, refAxis.getSoftLimitHigh()));
                    Location limit = toTransformed(axesLimit, LocationOption.Quiet);
                    limit = toHeadMountableLocation(limit, LocationOption.Quiet);
                    limit1 = limit.getRotation();
                }
            }
        }
        if (limit0 <= limit1) {
            return new double [] {limit0, limit1};
        }
        else {
            return new double [] {limit1, limit0};
        }
    }

    @Override
    public void prepareForPickAndPlaceArticulation(Location pickLocation, Location placementLocation) throws Exception {
        // Reset the current rotationModeOffset so all the calculating transforms are not affected.
        setRotationModeOffset(null);

        Double newRotationModeOffset = null;
        switch (getRotationMode()) {
            case AbsolutePartAngle:
                newRotationModeOffset = null;
                break;
            case PlacementAngle:
                newRotationModeOffset = placementLocation.getRotation();
                break;
            case MinimalRotation:
                newRotationModeOffset = Utils2D.angleNorm(getLocation().getRotation() - pickLocation.getRotation(), 180);
                break;
            case LimitedArticulation:
                double [] rotationLimits = getRotationModeLimits();
                double articulation = rotationLimits[1] - rotationLimits[0];
                // Axis has limited articulation, rotation is centered around the mid-range. 
                double pickToPlaceRotation = Utils2D.angleNorm(placementLocation.getRotation() - pickLocation.getRotation(), 180.0);
                // Allow for an additional 45° for alignment on the pick-to-place rotation. 
                double maximumRotation = pickToPlaceRotation + Math.signum(pickToPlaceRotation)*45;
                double angleStart;
                if (Math.abs(maximumRotation) < articulation) {
                    // The needed rotation is lower than the available articulation, therefore limit it 
                    // around the mid-point. 
                    double midPoint = (rotationLimits[0] + rotationLimits[1])*0.5;
                    angleStart = midPoint - maximumRotation*0.5;
                }
                else if (pickToPlaceRotation > 0) {
                    // A positive rotation is wanted. Start at the axis lower limit.
                    angleStart = rotationLimits[0];
                }
                else {
                    // A negative rotation is wanted. Start at the axis higher limit.
                    angleStart = rotationLimits[1];
                }
                newRotationModeOffset = 
                        Utils2D.angleNorm(pickLocation.getRotation() - angleStart, 180);
                break;
        }
        // Establish the new offset.
        setRotationModeOffset(newRotationModeOffset);
    }

    public Double getRotationModeOffset() {
        return rotationModeOffset;
    }

    public void setRotationModeOffset(Double rotationModeOffset) {
        Object oldValue = this.rotationModeOffset;
        this.rotationModeOffset = rotationModeOffset;
        firePropertyChange("rotationModeOffset", oldValue, rotationModeOffset);
        Logger.trace("Set rotation mode offset: "+rotationModeOffset != null ? rotationModeOffset+"°." : "none.");
        // Note, we do not 
        //  fireMachineHeadActivity(head); 
        // as only the upcoming coordinate changes will really make sense and matter.
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return Icons.captureTool;
    }
    
    protected void syncCompatibleNozzleTipIds() {
        compatibleNozzleTipIds.clear();
        for (NozzleTip nt : compatibleNozzleTips) {
            compatibleNozzleTipIds.add(nt.getId());
        }
    }

    @Override
    public Set<NozzleTip> getCompatibleNozzleTips() {
        if (compatibleNozzleTips == null) {
            compatibleNozzleTips = new HashSet<>();
            for (String nozzleTipId : compatibleNozzleTipIds) {
                NozzleTip nt = Configuration.get().getMachine().getNozzleTip(nozzleTipId);
                if (nt != null) {
                    compatibleNozzleTips.add(nt);
                }
            }
        }
        return Collections.unmodifiableSet(compatibleNozzleTips);
    }

    @Override
    public Set<NozzleTip> getCompatibleNozzleTips(Part part) {
        Set<NozzleTip> partCompatibleNozzleTips = new HashSet<>();
        if (part != null && part.getPackage() != null) {
            for (NozzleTip nt : getCompatibleNozzleTips()) {
                if (isNozzleTipAndPartCompatible(nt, part)) {
                    partCompatibleNozzleTips.add(nt);
                }
            }
        }
        return partCompatibleNozzleTips;
    }

    protected boolean isNozzleTipAndPartCompatible(NozzleTip nt, Part part) {
        return part.getPackage().getCompatibleNozzleTips().contains(nt);
    }

    @Override
    public void addCompatibleNozzleTip(NozzleTip nt) {
        // Makes sure the structure has been initialized.
        getCompatibleNozzleTips();
        compatibleNozzleTips.add(nt);
        syncCompatibleNozzleTipIds();
        firePropertyChange("compatibleNozzleTips", null, getCompatibleNozzleTips());
    }

    @Override
    public void removeCompatibleNozzleTip(NozzleTip nt) {
        // Makes sure the structure has been initialized.
        getCompatibleNozzleTips();
        compatibleNozzleTips.remove(nt);
        syncCompatibleNozzleTipIds();
        firePropertyChange("compatibleNozzleTips", null, getCompatibleNozzleTips());
    }
}
