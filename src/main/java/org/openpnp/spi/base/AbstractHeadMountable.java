package org.openpnp.spi.base;

import java.util.Arrays;

import org.openpnp.ConfigurationListener;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceVirtualAxis;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

public abstract class AbstractHeadMountable extends AbstractModelObject implements ReferenceHeadMountable {
    private AbstractAxis axisX;
    private AbstractAxis axisY;
    private AbstractAxis axisZ;
    private AbstractAxis axisRotation;

    @Attribute(required = false)
    private String axisXId;
    @Attribute(required = false)
    private String axisYId;
    @Attribute(required = false)
    private String axisZId;
    @Attribute(required = false)
    private String axisRotationId;

    public AbstractHeadMountable() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {

            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                axisX = (AbstractAxis) configuration.getMachine().getAxis(axisXId);
                axisY = (AbstractAxis) configuration.getMachine().getAxis(axisYId);
                axisZ = (AbstractAxis) configuration.getMachine().getAxis(axisZId);
                axisRotation = (AbstractAxis) configuration.getMachine().getAxis(axisRotationId);
            }
        });
    }

    @Override
    public AbstractAxis getAxisX() {
        return axisX;
    }
    public void setAxisX(AbstractAxis axisX) {
        assert axisX.getType() == Axis.Type.X;
        this.axisX = axisX;
        this.axisXId = (axisX == null) ? null : axisX.getId();
    }
    @Override
    public AbstractAxis getAxisY() {
        return axisY;
    }
    public void setAxisY(AbstractAxis axisY) {
        assert axisY.getType() == Axis.Type.Y;
        this.axisY = axisY;
        this.axisYId = (axisY == null) ? null : axisY.getId();
    }
    @Override
    public AbstractAxis getAxisZ() {
        return axisZ;
    }
    public void setAxisZ(AbstractAxis axisZ) {
        assert axisZ.getType() == Axis.Type.Z;
        this.axisZ = axisZ;
        this.axisZId = (axisZ == null) ? null : axisZ.getId();
    }
    @Override
    public AbstractAxis getAxisRotation() {
        return axisRotation;
    }
    public void setAxisRotation(AbstractAxis axisRotation) {
        assert axisRotation.getType() == Axis.Type.Rotation;
        this.axisRotation = axisRotation;
        this.axisRotationId = (axisRotation == null) ? null : axisRotation.getId();
    }

    @Override
    public  AbstractAxis getAxis(Axis.Type type) {
        switch (type) {
            case X:
                return getAxisX();
            case Y:
                return getAxisY();
            case Z:
                return getAxisZ();
            case Rotation:
                return getAxisRotation();
            default:
                return null;
        }
    }

    public  void setAxis(AbstractAxis axis, Axis.Type type) {
        switch (type) {
            case X:
                setAxisX(axis);
                break;
            case Y:
                setAxisY(axis);
                break;
            case Z:
                setAxisZ(axis);
                break;
            case Rotation:
                setAxisRotation(axis);
                break;
        }
    }

    public  void setAxis(AbstractAxis axis) {
        setAxis(axis, axis.getType());
    }

    protected CoordinateAxis getCoordinateAxisZ() {
        AbstractAxis zAxis = getAxisZ();
        if (zAxis != null) {
            Machine machine = Configuration.get().getMachine();
            // Get the raw Z Axis.
            try {
                return zAxis.getCoordinateAxes(machine).getAxis(Axis.Type.Z);
            }
            catch (Exception e) {
                // Cannot throw in this (legacy) context. 
                // However, this should never happen, as axis transforms cannot mix multiple Z axes together and
                // therefore the typed axis should be unique.
                Logger.error(e);
            }
        }
        return null;
    }


    protected Length rawToHeadMountableZ(ReferenceControllerAxis rawAxis, Length z) {
        // Get the raw location, and put z in it.
        AxesLocation rawLocation = getMappedAxes(Configuration.get().getMachine())
                .put(new AxesLocation(rawAxis, z));
        // Transform to Head coordinates.
        Location location = toTransformed(rawLocation);
        // From Head to HeadMountable coordinates.
        location = toHeadMountableLocation(location);
        return location.getLengthZ();
    }

    protected Length headMountableToRawZ(CoordinateAxis axisZ, Length z) throws Exception {
        // Take z as HeadMountable coordinate.
        Location location = getLocation();
        z = z.convertToUnits(location.getUnits());
        location = location.derive(null, null, z.getValue(), null);
        // Transform to Head coordinates.
        location = toHeadLocation(location);
        // From Head to to raw coordinates.
        AxesLocation rawLocation = toRaw(location);
        return rawLocation.getLengthCoordinate(axisZ);
    }

    @Override
    public Length [] getSafeZZone() {
        Length safeZLow = null;
        Length safeZHigh = null;
        CoordinateAxis coordAxis = getCoordinateAxisZ();
        if (coordAxis instanceof ReferenceControllerAxis) {
            ReferenceControllerAxis rawAxis = (ReferenceControllerAxis) coordAxis; 
            if (rawAxis.isSafeZoneLowEnabled()) {
                // We have a lower Safe Z Zone limit.
                Length z = rawAxis.getSafeZoneLow();
                safeZLow = rawToHeadMountableZ(rawAxis, z);
            }
            if (rawAxis.isSafeZoneHighEnabled()) {
                // We have a upper Safe Z Zone limit.
                Length z = rawAxis.getSafeZoneHigh();
                safeZHigh = rawToHeadMountableZ(rawAxis, z);
            }
            // Note, Z axis transform might be negative, so upper and lower limit may be swapped.
            // We can compare Lengths without unit conversion as they are in System units courtesy of 
            // rawToHeadMountableZ().
            if (safeZLow != null && safeZHigh != null 
                    && safeZLow.getValue() > safeZHigh.getValue()) {
                Length swap = safeZLow;
                safeZLow = safeZHigh;
                safeZHigh = swap;
            }
        }
        else if (coordAxis != null) {
            // Just take the home coordinate as Safe Z.
            safeZLow = safeZHigh = coordAxis.getHomeCoordinate();
        }
        return new Length[] { safeZLow, safeZHigh};
    }

    @Override
    public Length getSafeZ() {
        Length safeZ [] = getSafeZZone();
        return safeZ[0];
    }

    @Override
    public boolean isInSafeZZone(Length z) throws Exception {
        CoordinateAxis coordAxis = getCoordinateAxisZ();
        if (coordAxis != null) {
            Length rawZ = headMountableToRawZ(coordAxis, z);
            return coordAxis.isInSafeZone(rawZ);
        }
        else {
            return true;
        }
    }

    public void setSafeZ(Length safeZ) {
        // This is just a fake setter that seems to be needed when using addWrappedBinding(), even if the field is not editable.
        // Safe Z must now be set/captured on the Axis. 
    }

    @Override 
    public Length getEffectiveSafeZ() throws Exception {
        return getSafeZ();
    }

    @Override
    public void moveTo(Location location, double speed, MotionOption... options) throws Exception {
        Logger.debug("{}.moveTo({}, {})", getName(), location, speed);
        Location headLocation = toHeadLocation(location, getLocation());
        getHead().moveTo(this, headLocation, getHead().getMaxPartSpeed() * speed, options);
    }

    @Override
    public void moveToSafeZ(double speed) throws Exception {
        Location l = getLocation();
        Length safeZ = this.getEffectiveSafeZ();
        if (safeZ != null) {
            Logger.debug("{}.moveToSafeZ({})", getName(), speed);
            if (!isInSafeZZone(safeZ)) {
                throw new Exception("Effective Safe Z coordinate "+safeZ+" is outside Safe Z Zone.");
            }
            safeZ = safeZ.convertToUnits(l.getUnits());
            if (safeZ.getValue() > l.getZ() || !isInSafeZZone(l.getLengthZ())) {
                // Only move if 
                // a) the new effective Safe Z is higher than current Z, or 
                // b) the current Z is outside the safe zone.
                // The second condition must be checked for shared Z with negating transform, where the Safe Z Zone 
                // is limited on two sides. 
                l = l.derive(null, null, safeZ.getValue(), null);
                moveTo(l, speed);
            }
        }
    }

    @Override
    public void moveTo(Location location, MotionOption... options) throws Exception {
        moveTo(location, getHead().getMachine().getSpeed(), options);
    }

    @Override
    public void moveToSafeZ() throws Exception {
        moveToSafeZ(getHead().getMachine().getSpeed());
    }

    @Override
    public void waitForCompletion(CompletionType completionType) throws Exception {
        Machine machine = Configuration.get().getMachine();
        if (machine.isEnabled() && machine instanceof ReferenceMachine) {
            ((ReferenceMachine) machine)
                .getMotionPlanner().waitForCompletion(getHead() == null ? null : this, completionType);
        }
    }


    @Override
    public AxesLocation getMappedAxes(Machine machine) {
        return new AxesLocation((a, b) -> (a),
                AbstractAxis.getCoordinateAxes(axisX, machine),
                AbstractAxis.getCoordinateAxes(axisY, machine),
                AbstractAxis.getCoordinateAxes(axisZ, machine),
                AbstractAxis.getCoordinateAxes(axisRotation, machine));
    }

    public Location toHeadLocation(Location location, Location currentLocation, LocationOption... options) {
        // Shortcut Double.NaN. Sending Double.NaN in a Location is an old API that should no
        // longer be used. It will be removed eventually:
        // https://github.com/openpnp/openpnp/issues/255
        // In the mean time, since Double.NaN would cause a problem for transformations, we shortcut
        // it here by replacing any NaN values with the current value from the axes.
        location = location.derive(currentLocation, 
                Double.isNaN(location.getX()), 
                Double.isNaN(location.getY()), 
                Double.isNaN(location.getZ()), 
                Double.isNaN(location.getRotation())); 
        // Subtract the Head offset.
        location = location.subtract(getHeadOffsets());
        return location;
    }
    @Override
    public Location toHeadLocation(Location location, LocationOption... options) throws Exception {
        return toHeadLocation(location, getLocation(), options);
    }

    public Location toHeadMountableLocation(Location location, Location currentLocation, LocationOption... options) {
        if (currentLocation != null) {
            // Shortcut Double.NaN. Sending Double.NaN in a Location is an old API that should no
            // longer be used. It will be removed eventually:
            // https://github.com/openpnp/openpnp/issues/255
            // In the mean time, since Double.NaN would cause a problem for transformations, we shortcut
            // it here by replacing any NaN values with the current value from the axes.
            location = location.derive(currentLocation, 
                    Double.isNaN(location.getX()), 
                    Double.isNaN(location.getY()), 
                    Double.isNaN(location.getZ()), 
                    Double.isNaN(location.getRotation()));
        }
        // Add the Head offset.
        location = location.add(getHeadOffsets());
        return location;
    }
    @Override
    public Location toHeadMountableLocation(Location location, LocationOption... options) {
        return toHeadMountableLocation(location, null, options);
    }

    protected Location toMappedLocation(LengthUnit units, AxesLocation location) {
        double x = location.getLengthCoordinate(axisX).convertToUnits(units).getValue();  
        double y = location.getLengthCoordinate(axisY).convertToUnits(units).getValue();
        double z = location.getLengthCoordinate(axisZ).convertToUnits(units).getValue();
        double rotation = location.getCoordinate(axisRotation);
        return new Location(units, x, y, z, rotation);
    }

    protected AxesLocation toAxesLocation(Location location) {
        return new AxesLocation((a, b) -> (b),
                new AxesLocation(axisX, location.getLengthX()),
                new AxesLocation(axisY, location.getLengthY()),
                new AxesLocation(axisZ, location.getLengthZ()),
                new AxesLocation(axisRotation, location.getRotation()));
    }

    @Override
    public Location toTransformed(AxesLocation axesLocation, LocationOption... options) {
        axesLocation = AbstractTransformedAxis.toTransformed(axisX, axesLocation, options);
        axesLocation = AbstractTransformedAxis.toTransformed(axisY, axesLocation, options);
        axesLocation = AbstractTransformedAxis.toTransformed(axisZ, axesLocation, options);
        axesLocation = AbstractTransformedAxis.toTransformed(axisRotation, axesLocation, options);
        Location location = toMappedLocation(Configuration.get().getSystemUnits(), axesLocation);
        return location;
    }

    @Override
    public AxesLocation toRaw(Location location, LocationOption... options) 
            throws Exception {
        AxesLocation axesLocation = toAxesLocation(location);
        axesLocation = AbstractTransformedAxis.toRaw(axisX, axesLocation, options);
        axesLocation = AbstractTransformedAxis.toRaw(axisY, axesLocation, options);
        axesLocation = AbstractTransformedAxis.toRaw(axisZ, axesLocation, options);
        axesLocation = AbstractTransformedAxis.toRaw(axisRotation, axesLocation, options);
        return axesLocation;
    }

    @Override
    public Location getLocation() {
        AxesLocation axesLocation = getMappedAxes(Configuration.get().getMachine());
        Location location = toTransformed(axesLocation);
        // From head to HeadMountable.
        return toHeadMountableLocation(location);
    }

    @Override
    public Location getApproximativeLocation(Location currentLocation, Location desiredLocation, LocationOption... options) 
            throws Exception {
        // Inverse-transform the desired location to a raw location, applying the approximation options, which means some 
        // compensation effects are suppressed.
        Location desiredHeadLocation = toHeadLocation(desiredLocation, options);
        Location currentHeadLocation = toHeadLocation(currentLocation);
        AxesLocation desiredRawLocation = toRaw(desiredHeadLocation);
        AxesLocation currentRawLocation = toRaw(currentHeadLocation);
        // Evaluate the Keep options. 
        for (Axis axis : desiredRawLocation.getAxes()) {
            if (axis instanceof ControllerAxis) {
                ControllerAxis rawAxis = (ControllerAxis)axis;
                if ((rawAxis.getType() == Axis.Type.X && Arrays.asList(options).contains(LocationOption.KeepX)) 
                        && (rawAxis.getType() == Axis.Type.Y && Arrays.asList(options).contains(LocationOption.KeepY)) 
                        && (rawAxis.getType() == Axis.Type.Z && Arrays.asList(options).contains(LocationOption.KeepZ)) 
                        && (rawAxis.getType() == Axis.Type.Rotation && Arrays.asList(options).contains(LocationOption.KeepRotation))) {
                    desiredRawLocation = desiredRawLocation
                            .put(new AxesLocation(rawAxis, currentRawLocation.getCoordinate(rawAxis)));
                }
            }
            else if ((axis instanceof ReferenceVirtualAxis) 
                && (Arrays.asList(options).contains(LocationOption.ReplaceVirtual))) {
                // Replace the virtual axis coordinate with zero
                desiredRawLocation = desiredRawLocation
                        .put(new AxesLocation(axis, new Length(0.0, LengthUnit.Millimeters)));
            }
        }
        // Now transform it forward, NOT applying any options, i.e. when a moveTo() is later made, it will effectively reverse 
        // the option-less transformation and end up in the desired approximative location. 
        Location headApproximativeLocation = toTransformed(desiredRawLocation);
        Location approximativeLocation = toHeadMountableLocation(headApproximativeLocation);
        return approximativeLocation;
    }
}
