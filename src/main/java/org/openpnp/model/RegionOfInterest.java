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
    @Element(required = false)
    protected Location offset;
    
    public RegionOfInterest()
    {}
    
    public RegionOfInterest(Location upperLeftCorner, Location upperRightCorner,
            Location lowerLeftCorner, boolean rectify) {
        this.upperLeftCorner = upperLeftCorner;
        this.upperRightCorner = upperRightCorner;
        this.lowerLeftCorner = lowerLeftCorner;
        this.rectify = rectify;
        this.offset = null;
    }
    
    public RegionOfInterest(Location upperLeftCorner, Location upperRightCorner,
            Location lowerLeftCorner, boolean rectify, Location offset) {
        this.upperLeftCorner = upperLeftCorner;
        this.upperRightCorner = upperRightCorner;
        this.lowerLeftCorner = lowerLeftCorner;
        this.rectify = rectify;
        this.offset = offset;
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
    public Location getOffset() {
        return offset;
    }
    
    public RegionOfInterest rotateXy(double angle) {
        return new RegionOfInterest(
                upperLeftCorner.rotateXy(angle),
                upperRightCorner.rotateXy(angle),
                lowerLeftCorner.rotateXy(angle),
                rectify,
                this.offset==null ? null : offset.rotateXy(angle));
    }
}
