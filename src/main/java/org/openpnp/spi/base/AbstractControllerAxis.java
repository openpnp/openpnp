package org.openpnp.spi.base;

import org.openpnp.ConfigurationListener;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.MappedAxes;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Movable.LocationOption;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractControllerAxis extends AbstractAxis implements ControllerAxis {

    private Driver driver;

    @Attribute(required = false)
    private String driverId;

    @Attribute(required = false)
    private String letter;

    /**
     * The coordinate that will be reached when all pending motion has completed.
     */
    private double coordinate;

    /**
     * The coordinate that will be reached when the last motion sent by the driver has completed.
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
        setCoordinate(coordinate.convertToUnits(getUnits()).getValue());
    }

    @Override
    public MappedAxes getControllerAxes(Machine machine) {
        return new MappedAxes(this);
    }

    @Override
    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        Object oldValue = this.driver;
        this.driver = driver;
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
        double a = roundedToResolution(coordinateA.convertToUnits(getUnits()).getValue());
        double b = roundedToResolution(coordinateB.convertToUnits(getUnits()).getValue());
        return a == b;
    }


}
