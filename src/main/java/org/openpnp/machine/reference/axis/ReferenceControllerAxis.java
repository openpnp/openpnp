package org.openpnp.machine.reference.axis;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceControllerAxisConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class ReferenceControllerAxis extends AbstractControllerAxis {
    // The more implementation specific properties are in the ReferenceControllerAxis

    @Element(required = false)
    private Length backlashOffset = new Length(0.0, LengthUnit.Millimeters);

    @Element(required = false, data = true)
    private String preMoveCommand;

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceControllerAxisConfigurationWizard(this);
    }

    // Stored current axis coordinate.
    private double coordinate;

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

    public String getPreMoveCommand() {
        return preMoveCommand;
    }

    public void setPreMoveCommand(String preMoveCommand) {
        this.preMoveCommand = preMoveCommand;
    }

    public Length getBacklashOffset() {
        return backlashOffset;
    }

    public void setBacklashOffset(Length backlashOffset) {
        this.backlashOffset = backlashOffset;
    }

    @Override
    public AxesLocation toTransformed(AxesLocation location) {
        // No transformation, obviously
        return location;
    }

    @Override
    public AxesLocation toRaw(AxesLocation location) {
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
