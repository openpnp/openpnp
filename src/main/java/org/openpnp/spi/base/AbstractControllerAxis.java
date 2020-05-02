package org.openpnp.spi.base;

import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.ControllerAxis;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractControllerAxis extends AbstractAxis implements ControllerAxis {

    @Attribute(required = false)
    private String designator;
    
    @Element(required = false)
    private Length homeCoordinate = new Length(0.0, LengthUnit.Millimeters);

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
