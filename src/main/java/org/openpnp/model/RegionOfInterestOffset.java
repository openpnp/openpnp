package org.openpnp.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class RegionOfInterestOffset extends  RegionOfInterest{
    @Element(required = false)
    protected Location offset;

    public RegionOfInterestOffset()
    {}
    
    public RegionOfInterestOffset(Location upperLeftCorner, Location upperRightCorner,
            Location lowerLeftCorner, boolean rectify, Location offset) {
        super(upperLeftCorner, upperRightCorner, lowerLeftCorner, rectify);
        this.offset = offset;
    }
    public Location getOffset() {
        return offset;
    }
    
    public RegionOfInterestOffset rotateXy(double angle) {
        return new RegionOfInterestOffset(
                upperLeftCorner.rotateXy(angle),
                upperRightCorner.rotateXy(angle),
                lowerLeftCorner.rotateXy(angle),
                rectify,
                offset.rotateXy(angle));
    }
}
