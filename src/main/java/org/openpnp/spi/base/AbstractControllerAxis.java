package org.openpnp.spi.base;

import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.MappedAxes;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Machine;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractControllerAxis extends AbstractAxis implements ControllerAxis {

    private Driver driver;

    @Attribute(required = false)
    private String driverId;

    @Attribute(required = false)
    private String letter;

    @Element(required = false)
    private Length homeCoordinate = new Length(0.0, LengthUnit.Millimeters);

    @Element(required = false)
    private Length feedratePerSecond = new Length(250, LengthUnit.Millimeters);
    
    @Element(required = false)
    private Length accelerationPerSecond2 = new Length(500, LengthUnit.Millimeters);

    @Element(required = false)
    private Length jerkPerSecond3 = new Length(2000, LengthUnit.Millimeters);

    /**
     * The resolution of the axis will be used to determined if an axis has moved i.e. whether the sent coordinate 
     * will be different.  
     * @see %.4f format in CommandType.MOVE_TO_COMMAND in GcodeDriver.createDefaults() or Configuration.getLengthDisplayFormat()
     * Comparing coordinates rounded to resolution will also suppress false differences from floating point artifacts 
     * prompted by forward/backward raw <-> transformed calculations. 
     */
    @Element(required = false)
    private double resolution = 0.0001; // 

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
    public double roundedToResolution(double coordinate) {
        if (resolution != 0.0) {
            return Math.round(coordinate/resolution)*resolution;
        }
        else {
            return coordinate;
        }
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
    public Length getHomeCoordinate() {
        return homeCoordinate;
    }

    @Override
    public void setHomeCoordinate(Length homeCoordinate) {
        Object oldValue = this.homeCoordinate;
        this.homeCoordinate = homeCoordinate;
        firePropertyChange("homeCoordinate", oldValue, homeCoordinate);
    }

    public double getResolution() {
        return resolution;
    }

    public void setResolution(double resolution) {
        Object oldValue = this.resolution;
        this.resolution = resolution;
        firePropertyChange("resolution", oldValue, resolution);
    }

    public Length getFeedratePerSecond() {
        return feedratePerSecond;
    }

    public void setFeedratePerSecond(Length feedratePerSecond) {
        this.feedratePerSecond = feedratePerSecond;
    }

    public Length getAccelerationPerSecond2() {
        return accelerationPerSecond2;
    }

    public void setAccelerationPerSecond2(Length accelerationPerSecond2) {
        this.accelerationPerSecond2 = accelerationPerSecond2;
    }

    public Length getJerkPerSecond3() {
        return jerkPerSecond3;
    }

    public void setJerkPerSecond3(Length jerkPerSecond3) {
        this.jerkPerSecond3 = jerkPerSecond3;
    }

    @Override
    public double getMotionLimit(int order) {
        if (order == 1) {
            return getFeedratePerSecond().convertToUnits(LengthUnit.Millimeters).getValue();
        }
        else if (order == 2) {
            return getAccelerationPerSecond2().convertToUnits(LengthUnit.Millimeters).getValue();
        }
        else if (order == 3) {
            return getJerkPerSecond3().convertToUnits(LengthUnit.Millimeters).getValue();
        }
        return 0;
    }
}
