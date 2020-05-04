package org.openpnp.machine.reference.axis;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceCamMasterAxisConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Axis;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractTransformedAxis;
import org.openpnp.spi.base.AbstractSingleTransformedAxis;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * A TransformedAxis for heads with dual rocker or seesaw driven Z axes powered by one motor. The two Z 
 * axes are defined as Master and Slave. 
 * Master gets the positive rotation of the axis motor as positive Z (up). Slave gets the negated transform. 
 */
public class ReferenceCamMasterAxis extends AbstractSingleTransformedAxis {

    @Element(required = false)
    private Length camRadius = new Length(24.0, LengthUnit.Millimeters);

    @Element(required = false)
    private Length camWheelRadius = new Length(9.5, LengthUnit.Millimeters);

    @Element(required = false)
    private Length camWheelGap = new Length(2, LengthUnit.Millimeters);

    public ReferenceCamMasterAxis() {
        super();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceCamMasterAxisConfigurationWizard((AbstractMachine)Configuration.get().getMachine(), this);
    }

    @Override
    public Location transformToRaw(Location location) {
        return location;
    }

    @Override
    public Location transformFromRaw(Location location) {
        // it's reversible
        return transformToRaw(location);
    }

    public Length getCamRadius() {
        return camRadius;
    }

    public void setCamRadius(Length camRadius) {
        this.camRadius = camRadius;
    }

    public Length getCamWheelRadius() {
        return camWheelRadius;
    }

    public void setCamWheelRadius(Length camWheelRadius) {
        this.camWheelRadius = camWheelRadius;
    }

    public Length getCamWheelGap() {
        return camWheelGap;
    }

    public void setCamWheelGap(Length camWheelGap) {
        this.camWheelGap = camWheelGap;
    }
    
}
