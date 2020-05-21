package org.openpnp.spi.base;

import org.openpnp.ConfigurationListener;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Movable.LocationOption;
import org.openpnp.util.MovableUtils;
import org.simpleframework.xml.Attribute;

public abstract class AbstractControllerAxis extends AbstractAxis implements ControllerAxis {

    private Driver driver;

    @Attribute(required = false)
    private String driverId;

    @Attribute(required = false)
    private String letter;

    /**
     * The coordinate that will be reached when all pending motion has completed. 
     * Always in driver units.
     */
    private double coordinate;

    /**
     * The coordinate that will be reached when the last motion sent by the driver has completed.
     * Always in driver units.
     * 
     */
    private double driverCoordinate;

    protected AbstractControllerAxis () {
        super();
        Configuration.get().addListener(new ConfigurationListener.Adapter() {

            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                driver = configuration.getMachine().getDriver(driverId);
            }
        });    
    }

    @Override
    public double getCoordinate() {
        return coordinate;
    }
    @Override
    public Length getLengthCoordinate() {
        return new Length(coordinate, getUnits());
    }

    @Override
    public void setCoordinate(double coordinate) {
        Object oldValue = this.coordinate;
        this.coordinate = coordinate;
        firePropertyChange("coordinate", oldValue, coordinate);
        firePropertyChange("lengthCoordinate", null, getLengthCoordinate());
    }
    @Override
    public void setLengthCoordinate(Length coordinate) {
        if (getType() == Type.Rotation) {
            // Never convert rotation angles.
            setCoordinate(coordinate.getValue());
        }
        else {
            setCoordinate(coordinate.convertToUnits(AxesLocation.getUnits()).getValue());
        }
    }

    @Override
    public AxesLocation getCoordinateAxes(Machine machine) {
        return new AxesLocation(this);
    }

    @Override
    public double getDriverCoordinate() {
        return driverCoordinate;
    }
    @Override
    public Length getDriverLengthCoordinate() {
        return new Length(driverCoordinate, getUnits());
    }

    @Override
    public void setDriverCoordinate(double driverCoordinate) {
        Object oldValue = this.driverCoordinate;
        this.driverCoordinate = driverCoordinate;
        firePropertyChange("driverCoordinate", oldValue, driverCoordinate);
        firePropertyChange("driverLengthCoordinate", null, getDriverLengthCoordinate());
    }
    @Override
    public void setDriverLengthCoordinate(Length driverCoordinate) {
        if (getType() == Type.Rotation) {
            // Never convert rotation angles.
            setDriverCoordinate(driverCoordinate.getValue());
        }
        else {
            setDriverCoordinate(driverCoordinate.convertToUnits(getUnits()).getValue());
        }
    }

    @Override
    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        Object oldValue = this.driver;
        this.driver = (ReferenceDriver) driver;
        this.driverId = (driver == null) ? null : driver.getId();
        firePropertyChange("driver", oldValue, driver);
    }

    @Override
    public LengthUnit getUnits() {
        if (getDriver() != null) {
            return getDriver().getUnits();
        }
        return LengthUnit.Millimeters;
    }

    @Override
    public String getLetter() {
        return letter;
    }

    @Override
    public void setLetter(String letter) {
        Object oldValue = this.letter;
        this.letter = letter;
        firePropertyChange("letter", oldValue, letter);
    }

    @Override
    public AxesLocation toTransformed(AxesLocation location, LocationOption... options) {
        // No transformation, obviously
        return location;
    }

    @Override
    public AxesLocation toRaw(AxesLocation location, LocationOption... options) {
        // No transformation, obviously
        return location;
    }

    @Override
    public boolean coordinatesMatch(Length coordinateA, Length coordinateB) {
        return coordinatesMatch(
                coordinateA.convertToUnits(getUnits()).getValue(),
                coordinateB.convertToUnits(getUnits()).getValue());
    }

    @Override
    public boolean coordinatesMatch(double coordinateA, double coordinateB) {
        double a = roundedToResolution(coordinateA);
        double b = roundedToResolution(coordinateB);
        return a == b;
    }

    /**
     * Tries to move the axis to the specified raw coordinate in a safe way. 
     * 
     * @param coordinate
     * @throws Exception
     */
    public void moveAxis(Length coordinate) throws Exception {
        // To be safe we need to go through a HeadMountable and the full motion stack.
        // Find one that maps the axis.
        HeadMountable axisMover = null;
        for (HeadMountable hm : Configuration.get().getMachine().getDefaultHead().getHeadMountables()) {
            if (hm.getMappedAxes(Configuration.get().getMachine()).contains(this)) {    
                axisMover = hm;
                break;
            }
        }
        if (axisMover == null) {
            throw new Exception("The axis "+getName()+" is not mapped to any HeadMountables. Can't move safely.");
        }
        axisMover.moveToSafeZ();
        AxesLocation axesLocation = axisMover.toRaw(axisMover.toHeadLocation(axisMover.getLocation()))
                .put(new AxesLocation(this, coordinate));
        Location location = axisMover.toHeadMountableLocation(axisMover.toTransformed(axesLocation));
        MovableUtils.moveToLocationAtSafeZ(axisMover, location);
    }

}
