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
    protected Location offsets;
    
    public RegionOfInterest()
    {}
    
    public RegionOfInterest(Location upperLeftCorner, Location upperRightCorner,
            Location lowerLeftCorner, boolean rectify) {
        this.upperLeftCorner = upperLeftCorner;
        this.upperRightCorner = upperRightCorner;
        this.lowerLeftCorner = lowerLeftCorner;
        this.rectify = rectify;
        this.offsets = null;
    }
    
    public RegionOfInterest(Location upperLeftCorner, Location upperRightCorner,
            Location lowerLeftCorner, boolean rectify, Location offsets) {
        this.upperLeftCorner = upperLeftCorner;
        this.upperRightCorner = upperRightCorner;
        this.lowerLeftCorner = lowerLeftCorner;
        this.rectify = rectify;
        this.offsets = offsets;
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
    public Location getOffsets() {
        return offsets;
    }
    
    public RegionOfInterest rotateXy(double angle) {
        return new RegionOfInterest(
                upperLeftCorner.rotateXy(angle),
                upperRightCorner.rotateXy(angle),
                lowerLeftCorner.rotateXy(angle),
                rectify,
                this.offsets==null ? null : offsets.rotateXy(angle));
    }

    public void applyOffset(Location offset) {
        this.upperLeftCorner = this.upperLeftCorner.addWithRotation(offset);
        this.upperRightCorner = this.upperRightCorner.addWithRotation(offset);
        this.lowerLeftCorner = this.lowerLeftCorner.addWithRotation(offset);
    }
}
