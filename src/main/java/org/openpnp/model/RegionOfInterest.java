package org.openpnp.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class RegionOfInterest {
    @Element
    protected Location upperLeftCorner;
    @Element
    protected Location upperRightCorner;
    @Element
    protected Location lowerLeftCorner;
    @Attribute
    protected boolean rectify;

    public RegionOfInterest()
    {}
    
    public RegionOfInterest(Location upperLeftCorner, Location upperRightCorner,
            Location lowerLeftCorner, boolean rectify) {
        super();
        this.upperLeftCorner = upperLeftCorner;
        this.upperRightCorner = upperRightCorner;
        this.lowerLeftCorner = lowerLeftCorner;
        this.rectify = rectify;
    }
    public Location getUpperLeftCorner() {
        return upperLeftCorner;
    }
    public Location getUpperRightCorner() {
        return upperRightCorner;
    }
    public Location getLowerLeftCorner() {
        return lowerLeftCorner;
    }
    public boolean isRectify() {
        return rectify;
    }
    
    public RegionOfInterest rotateXy(double angle) {
        return new RegionOfInterest(
                upperLeftCorner.rotateXy(angle),
                upperRightCorner.rotateXy(angle),
                lowerLeftCorner.rotateXy(angle),
                rectify);
    }
}
