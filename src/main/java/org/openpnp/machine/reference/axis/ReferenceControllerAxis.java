package org.openpnp.machine.reference.axis;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceControllerAxisConfigurationWizard;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.base.AbstractControllerAxis;

public class ReferenceControllerAxis extends AbstractControllerAxis {
    
    private double coordinate;
    
     
    
    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceControllerAxisConfigurationWizard(this);
    }

    @Override
    public LengthUnit getUnits() {
        if (getDriver() != null) {
            return getDriver().getUnits();
        }
        return LengthUnit.Millimeters;
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
    public double toTransformed(Location location) {
        return getLocationAxisCoordinate(location);
    }

    @Override
    public double toRaw(Location location, double[][] invertedAffineTransform) {
        return getLocationAxisCoordinate(location);
    }

    @Override
    public boolean locationCoordinateMatches(Location locationA, Location locationB) {
        double coordinateA = roundedToResolution(getLocationAxisCoordinate(locationA.convertToUnits(getUnits())));
        double coordinateB = roundedToResolution(getLocationAxisCoordinate(locationB.convertToUnits(getUnits())));
        return coordinateA == coordinateB;
    }
}
