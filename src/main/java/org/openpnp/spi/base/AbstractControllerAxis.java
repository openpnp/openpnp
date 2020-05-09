package org.openpnp.spi.base;

import org.openpnp.ConfigurationListener;
import org.openpnp.machine.reference.driver.GcodeDriver.CommandType;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractControllerAxis extends AbstractAxis implements ControllerAxis {

    private Driver driver;
    
    @Attribute(required = false)
    private String driverId;

    @Attribute(required = false)
    private String designator;
    
    @Element(required = false)
    private Length homeCoordinate = new Length(0.0, LengthUnit.Millimeters);

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

    public double roundedToResolution(double coordinate) {
        if (resolution != 0.0) {
            return Math.round(coordinate/resolution)*resolution;
        }
        else {
            return coordinate;
        }
    }
    
    @Override
    public AbstractControllerAxis getControllerAxis() {
        return this;
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
    public String getDesignator() {
        return designator;
    }

    @Override
    public void setDesignator(String designator) {
        Object oldValue = this.designator;
        this.designator = designator;
        firePropertyChange("designator", oldValue, designator);
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
}
