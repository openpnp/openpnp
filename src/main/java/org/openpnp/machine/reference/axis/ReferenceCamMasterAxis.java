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
 * A TransformedAxis for heads with dual rocker or seesaw driven Z axes powered by one motor. 
 * The two Z axes are defined as Master and Slave. 
 * Master gets the positive rotation of the axis motor as positive Z (up). Slave gets the negative. 
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
    public double toRaw(Location location, double [][] invertedAffineTransform) {
        return toRaw(location, false);
    }

    protected double toRaw(Location location, boolean slave) {
        double transformedCoordinate = getLocationAxisCoordinate(location);
        double rawCoordinate = (transformedCoordinate 
                - camWheelRadius.convertToUnits(location.getUnits()).getValue() 
                - camWheelGap.convertToUnits(location.getUnits()).getValue()) 
                / camRadius.convertToUnits(location.getUnits()).getValue();
        rawCoordinate = Math.min(Math.max(rawCoordinate, -1), 1);
        rawCoordinate = Math.toDegrees(Math.asin(rawCoordinate));
        if (slave) {
            rawCoordinate = -rawCoordinate;
        }
        return rawCoordinate;
    }

    @Override
    public double toTransformed(Location location) {
        return toTransformed(location, false);
    }

    protected double toTransformed(Location location, boolean slave) {
        double rawCoordinate = getLocationAxisCoordinate(location);
        double transformedCoordinate = Math.sin(Math.toRadians(rawCoordinate)) 
                * camRadius.convertToUnits(location.getUnits()).getValue();
        if (slave) {
            transformedCoordinate = -transformedCoordinate;
        }
        transformedCoordinate += camWheelRadius.convertToUnits(location.getUnits()).getValue() 
                + camWheelGap.convertToUnits(location.getUnits()).getValue();
        return transformedCoordinate;
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
