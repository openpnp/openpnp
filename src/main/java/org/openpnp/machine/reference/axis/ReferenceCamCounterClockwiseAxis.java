package org.openpnp.machine.reference.axis;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceCamCounterClockwiseAxisConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Movable.LocationOption;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractTransformedAxis;
import org.openpnp.spi.base.AbstractSingleTransformedAxis;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * A TransformedAxis for heads with dual rocker or seesaw driven Z axes powered by one motor. 
 * The two Z axes are defined as counter-clockwise and clockwise according how the rocker rotates. 
 * 
 */
public class ReferenceCamCounterClockwiseAxis extends AbstractSingleTransformedAxis {

    @Element(required = false)
    private Length camRadius = new Length(24.0, LengthUnit.Millimeters);

    @Element(required = false)
    private Length camWheelRadius = new Length(9.5, LengthUnit.Millimeters);

    @Element(required = false)
    private Length camWheelGap = new Length(2, LengthUnit.Millimeters);

    public ReferenceCamCounterClockwiseAxis() {
        super();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceCamCounterClockwiseAxisConfigurationWizard((AbstractMachine)Configuration.get().getMachine(), this);
    }

    @Override
    public AxesLocation toRaw(AxesLocation location, LocationOption... options) throws Exception {
        if (inputAxis == null) {
            throw new Exception(getName()+" has no input axis set");
        }
        double transformedCoordinate = location.getCoordinate(this);
        double rawCoordinate = toRawCoordinate(transformedCoordinate, false);
        // store the new coordinate
        location = location.put(new AxesLocation(inputAxis, rawCoordinate));
        // recurse
        return inputAxis.toRaw(location, options);
    }

    protected double toRawCoordinate(double transformedCoordinate, boolean clockwise) throws Exception {
        double rawCoordinate = (transformedCoordinate 
                - camWheelRadius.convertToUnits(AxesLocation.getUnits()).getValue() 
                - camWheelGap.convertToUnits(AxesLocation.getUnits()).getValue()) 
                / camRadius.convertToUnits(AxesLocation.getUnits()).getValue();
        rawCoordinate = Math.min(Math.max(rawCoordinate, -1), 1);
        rawCoordinate = Math.toDegrees(Math.asin(rawCoordinate));
        if (clockwise) {
            rawCoordinate = -rawCoordinate;
        }
        // recurse
        return rawCoordinate;
    }

    @Override
    public AxesLocation toTransformed(AxesLocation location, LocationOption... options)  {
        if (inputAxis == null) {
            return location.put(new AxesLocation(this, 0.0));
        }
        // recurse
        location = inputAxis.toTransformed(location, options);
        double rawCoordinate = location.getCoordinate(inputAxis);
        double transformedCoordinate  = toTransformedCoordinate(rawCoordinate, false);
        return location.put(new AxesLocation(this, transformedCoordinate));
    }

    protected double toTransformedCoordinate(double rawCoordinate, boolean clockwise) {
        double transformedCoordinate = Math.sin(Math.toRadians(rawCoordinate)) 
                * camRadius.convertToUnits(AxesLocation.getUnits()).getValue();
        if (clockwise) {
            transformedCoordinate = -transformedCoordinate;
        }
        transformedCoordinate += camWheelRadius.convertToUnits(AxesLocation.getUnits()).getValue() 
                + camWheelGap.convertToUnits(AxesLocation.getUnits()).getValue();
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
