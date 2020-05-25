package org.openpnp.spi.base;

import org.openpnp.model.AxesLocation;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Axis;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Movable.LocationOption;
import org.simpleframework.xml.Element;

public abstract class AbstractCoordinateAxis extends AbstractAxis implements CoordinateAxis {
    @Element(required = false)
    private Length homeCoordinate = new Length(0.0, LengthUnit.Millimeters);

    /**
     * The coordinate that will be reached when all pending motion has completed. 
     * Always in driver units.
     */
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
        if (type == Type.Rotation) {
            // Never convert rotation angles.
            setCoordinate(coordinate.getValue());
        }
        else {
            setCoordinate(coordinate.convertToUnits(AxesLocation.getUnits()).getValue());
        }
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

    @Override
    public AxesLocation getCoordinateAxes(Machine machine) {
        return new AxesLocation(this);
    }

    @Override
    public AxesLocation toTransformed(AxesLocation location, LocationOption... options) {
        // No transformation, obviously
        return location;
    }

    @Override
    public AxesLocation toRaw(AxesLocation location, LocationOption... options) {
        // No transformation, obviously
        return location;
    }

    @Override
    public boolean coordinatesMatch(Length coordinateA, Length coordinateB) {
        if (type == Axis.Type.Rotation) {
            // Never convert rotation
            return coordinatesMatch(
                    coordinateA.getValue(),
                    coordinateB.getValue());
        }
        else {
            return coordinatesMatch(
                coordinateA.convertToUnits(getUnits()).getValue(),
                coordinateB.convertToUnits(getUnits()).getValue());
        }
    }
    
    @Override
    public boolean coordinatesMatch(double coordinateA, double coordinateB) {
        if (type == Axis.Type.Rotation) {
            long a = getResolutionTicks(coordinateA);
            long b = getResolutionTicks(coordinateB);
            long wraparound = getResolutionTicks(360.0);
            return (Math.abs(a - b) % wraparound) == 0;
        }
        else {
            long a = getResolutionTicks(coordinateA);
            long b = getResolutionTicks(coordinateB);
            return a == b;
        }
    }
    
    protected abstract long getResolutionTicks(double coordinate);
}
