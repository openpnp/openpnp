package org.openpnp.spi.base;

import org.openpnp.ConfigurationListener;
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

    protected AbstractControllerAxis () {
        super();
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
    
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                driver = configuration.getMachine().getDriver(driverId);
            }
        });    
    }

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
}
