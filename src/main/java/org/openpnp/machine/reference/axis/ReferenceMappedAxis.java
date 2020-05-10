package org.openpnp.machine.reference.axis;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceMappedAxisConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Axis;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractTransformedAxis;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Element;
import org.openpnp.spi.base.AbstractSingleTransformedAxis;

/**
 * A TransformedAxis for heads with dual linear Z axes powered by one motor. The two Z axes are
 * defined as normal and negated. Normal gets the raw coordinate value and negated gets the same
 * value negated. So, as normal moves up, negated moves down.
 */
public class ReferenceMappedAxis extends AbstractSingleTransformedAxis {
    @Element(required = false)
    private Length mapInput0 = new Length(0.0, LengthUnit.Millimeters);

    @Element(required = false)
    private Length mapOutput0 = new Length(0.0, LengthUnit.Millimeters);

    @Element(required = false)
    private Length mapInput1 = new Length(1.0, LengthUnit.Millimeters);

    @Element(required = false)
    private Length mapOutput1 = new Length(1.0, LengthUnit.Millimeters);

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceMappedAxisConfigurationWizard((AbstractMachine)Configuration.get().getMachine(), this);
    }

    @Override
    public double toRaw(Location location, double [][] invertedAffineTransform) {
        if (inputAxis != null) {
            double coordinate = 0.0;
            switch(type) {
                case X:
                    coordinate = location.getX();
                case Y:
                    coordinate = location.getY();
                case Z:
                    coordinate = location.getZ();
                case Rotation:
                    coordinate = location.getRotation();
            }

            // To raw, i.e. reversed mapped transform:
            double scale = getScale(location);
            coordinate = coordinate - mapOutput0.convertToUnits(location.getUnits()).getValue();
            coordinate = coordinate / scale; 
            coordinate = coordinate + mapInput0.convertToUnits(location.getUnits()).getValue(); 

            switch(type) {
                case X:
                    return inputAxis.toRaw(location.derive(-location.getX(), null, null, null), invertedAffineTransform);
                case Y:
                    return inputAxis.toRaw(location.derive(null, -location.getY(), null, null), invertedAffineTransform);
                case Z:
                    return inputAxis.toRaw(location.derive(null, null, -location.getZ(),  null), invertedAffineTransform);
                case Rotation:
                    return inputAxis.toRaw(location.derive(null, null, null, -location.getRotation()), invertedAffineTransform);
            }
        }
        return 0.0;
    }

    protected double getScale(Location location) {
        double dividend = (mapOutput1.convertToUnits(location.getUnits()).getValue() - mapOutput0.convertToUnits(location.getUnits()).getValue()); 
        double divisor = (mapInput1.convertToUnits(location.getUnits()).getValue() - mapInput0.convertToUnits(location.getUnits()).getValue());
        if (divisor == 0.0 || dividend == 0.0) {
            Logger.info("[ReferenceMappedAxis] "+getName()+" input/output range must not be zero. Scale defaults to 1.");
            return 1.0;
        }
        return dividend / divisor;
    }

    @Override
    public double toTransformed(Location location) {
        if (inputAxis != null) {
            double coordinate = inputAxis.toTransformed(location);

            // To transformed, i.e. forward mapped transform:
            double scale = getScale(location);
            coordinate = coordinate - mapInput0.convertToUnits(location.getUnits()).getValue();
            coordinate = coordinate * scale; 
            coordinate = coordinate + mapOutput0.convertToUnits(location.getUnits()).getValue(); 

            return coordinate;
        }
        return 0.0;
    }

    public Length getMapInput0() {
        return mapInput0;
    }

    public void setMapInput0(Length mapInput0) {
        this.mapInput0 = mapInput0;
    }

    public Length getMapOutput0() {
        return mapOutput0;
    }

    public void setMapOutput0(Length mapOutput0) {
        this.mapOutput0 = mapOutput0;
    }

    public Length getMapInput1() {
        return mapInput1;
    }

    public void setMapInput1(Length mapInput1) {
        this.mapInput1 = mapInput1;
    }

    public Length getMapOutput1() {
        return mapOutput1;
    }

    public void setMapOutput1(Length mapOutput1) {
        this.mapOutput1 = mapOutput1;
    }
}
