package org.openpnp.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class RegionOfInterestLocation extends  RegionOfInterest{
    protected Location location;

    public RegionOfInterestLocation()
    {}
    
    public RegionOfInterestLocation(Location upperLeftCorner, Location upperRightCorner,
            Location lowerLeftCorner, boolean rectify, Location location) {
        super(upperLeftCorner, upperRightCorner, lowerLeftCorner, rectify);
        this.location = location;
    }
    public Location getLocation() {
        return location;
    }
}
