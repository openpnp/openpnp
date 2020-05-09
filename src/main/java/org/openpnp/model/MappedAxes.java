package org.openpnp.model;

import java.util.ArrayList;
import java.util.List;

import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;

/**
 * The set of ControllerAxes mapped for a specific operation e.g. for a moveTo() or home().
 * This map is given to the drivers and each driver should perform the operation for those
 * axes mapped to it (i.e. where axis.getDriver() == this).   
 *  * 
 */
public class MappedAxes {
    private final ControllerAxis axisX;
    private final ControllerAxis axisY;
    private final ControllerAxis axisZ;
    private final ControllerAxis axisRotation;

    public MappedAxes(ControllerAxis axisX, ControllerAxis axisY, ControllerAxis axisZ,
            ControllerAxis axisRotation) {
        super();
        this.axisX = axisX;
        this.axisY = axisY;
        this.axisZ = axisZ;
        this.axisRotation = axisRotation;
    }
    public ControllerAxis getAxisX() {
        return axisX;
    }
    public ControllerAxis getAxisY() {
        return axisY;
    }
    public ControllerAxis getAxisZ() {
        return axisZ;
    }
    public ControllerAxis getAxisRotation() {
        return axisRotation;
    }
    public ControllerAxis getAxis(Axis.Type type) {
        switch (type) {
            case X:
                return axisX;
            case Y:
                return axisY;
            case Z:
                return axisZ;
            case Rotation:
                return axisRotation;
            default:
                return null;
        }
    }

    public ControllerAxis getAxis(Axis.Type type, Driver driver) {
        ControllerAxis axis = getAxis(type);
        if (axis != null && axis.getDriver() == driver) {
            return axis;
        }
        return null;
    }

    public Length getX(Driver driver) {
        if (axisX != null && (axisX.getDriver() == driver || driver == null)) {
            return axisX.getLengthCoordinate();
        }
        return new Length(0 , LengthUnit.Millimeters);
    }
    public void setX(Length x, Driver driver) {
        if (axisX != null && (axisX.getDriver() == driver || driver == null)) {
            axisX.setLengthCoordinate(x);
        }
    }
    public Length getY(Driver driver) {
        if (axisY != null && (axisY.getDriver() == driver || driver == null)) {
            return axisY.getLengthCoordinate();
        }
        return new Length(0 , LengthUnit.Millimeters);
    }
    public void setY(Length y, Driver driver) {
        if (axisY != null && (axisY.getDriver() == driver || driver == null)) {
            axisY.setLengthCoordinate(y);
        }
    }
    public Length getZ(Driver driver) {
        if (axisZ != null && (axisZ.getDriver() == driver || driver == null)) {
            return axisZ.getLengthCoordinate();
        }
        return new Length(0 , LengthUnit.Millimeters);
    }
    public void setZ(Length z, Driver driver) {
        if (axisZ != null && (axisZ.getDriver() == driver || driver == null)) {
            axisZ.setLengthCoordinate(z);
        }
    }
    public double getRotation(Driver driver) {
        if (axisRotation != null && (axisRotation.getDriver() == driver || driver == null)) {
            return axisRotation.getCoordinate();
        }
        return 0.0;
    }
    public void setRotation(double rotation, Driver driver) {
        if (axisRotation != null && (axisRotation.getDriver() == driver || driver == null)) {
            axisRotation.setCoordinate(rotation);
        }
    }

    public Length getHomeX() {
        if (axisX != null) {
            return axisX.getHomeCoordinate();
        }
        return new Length(0 , LengthUnit.Millimeters);
    }
    public Length getHomeY() {
        if (axisY != null) {
            return axisY.getHomeCoordinate();
        }
        return new Length(0 , LengthUnit.Millimeters);
    }
    public Length getHomeZ() {
        if (axisZ != null) {
            return axisZ.getHomeCoordinate();
        }
        return new Length(0 , LengthUnit.Millimeters);
    }
    public double getHomeRotation() {
        if (axisRotation != null) {
            return axisRotation.getHomeCoordinate().getValue();
        }
        return 0.0;
    }

    public Location getLocation(Driver driver) {
        // As each axis can be driven by a different driver, they could (theoretically) have different
        // LengthUnits. Convert everything into system units.
        LengthUnit lengthUnit = Configuration.get().getSystemUnits();
        return new Location(lengthUnit, 
                getX(driver).convertToUnits(lengthUnit).getValue(), 
                getY(driver).convertToUnits(lengthUnit).getValue(), 
                getZ(driver).convertToUnits(lengthUnit).getValue(), 
                getRotation(driver)); 
    }

    public void setLocation(Location location, Driver driver) {
        setX(location.getLengthX(), driver); 
        setY(location.getLengthY(), driver); 
        setZ(location.getLengthZ(), driver); 
        setRotation(location.getRotation(), driver); 
    }
   
    public Location getHomeLocation() {
        LengthUnit lengthUnit = Configuration.get().getSystemUnits();
        return new Location(lengthUnit, 
                getHomeX().convertToUnits(lengthUnit).getValue(), 
                getHomeY().convertToUnits(lengthUnit).getValue(), 
                getHomeZ().convertToUnits(lengthUnit).getValue(), 
                getHomeRotation()); 
    }

    public List<ControllerAxis> getAxes() {
        List<ControllerAxis> list = new ArrayList<>();
        if (axisX != null) {
            list.add(axisX);
        }
        if (axisY != null) {
            list.add(axisY);
        }
        if (axisZ != null) {
            list.add(axisZ);
        }
        if (axisRotation != null) {
            list.add(axisRotation);
        }
        return list;
    }

    public List<ControllerAxis> getAxes(Driver driver) {
        List<ControllerAxis> list = new ArrayList<>();
        if (axisX != null && axisX.getDriver() == driver) {
            list.add(axisX);
        }
        if (axisY != null && axisY.getDriver() == driver) {
            list.add(axisY);
        }
        if (axisZ != null && axisZ.getDriver() == driver) {
            list.add(axisZ);
        }
        if (axisRotation != null && axisRotation.getDriver() == driver) {
            list.add(axisRotation);
        }
        return list;
    }

    public List<Driver> getMappedDrivers() {
        List<Driver> list = new ArrayList<>();
        List<ControllerAxis> axes = getAxes();
        // Enumerate drivers in the order they are defined in the machine.
        // This is important, so the order of commands is independent of the 
        // axes that happen to be mapped. 
        for (Driver driver : Configuration.get().getMachine().getDrivers()) {
            // Check if one or more of the axes are mapped to the driver.
            for (ControllerAxis axis : axes) {
                if (driver == axis.getDriver()) {
                    list.add(driver);
                    break;
                }
            }
        }
        return list;
    }
    public boolean locationMatches(Location locationA, Location locationB, Driver driver) {
        for (ControllerAxis axis : getAxes(driver)) {
            if (!axis.locationCoordinateMatches(locationA, locationB)) {
                return false;
            }
        }
        return true;
    }
    public boolean isEmpty() {
        return axisX == null 
                && axisY == null
                        && axisZ == null
                                && axisRotation == null;
    }
}
