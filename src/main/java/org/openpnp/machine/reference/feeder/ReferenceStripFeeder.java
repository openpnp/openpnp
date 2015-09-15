/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.feeder;



import javax.swing.Action;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferenceStripFeederConfigurationWizard;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of Feeder that indexes through a strip of cut tape.
 * This is a specialization of the tray feeder that knows specifics
 * about tape so that vision capabilities can be added.
 */

/**
 * SMD tape standard info from http://www.liteplacer.com/setup-tape-positions-2/
 * holes 1.5mm
 * hole pitch 4mm
 * first part center to reference hole linear is 2mm
 * tape width is multiple of 4mm
 * part pitch is multiple of 4mm except for 0402 and smaller, where it is 2mm 
 * hole to part lateral is tape width / 2 - 0.5mm 
 */
public class ReferenceStripFeeder extends ReferenceFeeder {
	private final static Logger logger = LoggerFactory.getLogger(ReferenceStripFeeder.class);
	
	public enum TapeType {
	    WhitePaper("White Paper"),
	    BlackPlastic("Black Plastic"),
	    ClearPlastic("Clear Plastic");
	    
	    private String name;
	    
	    TapeType(String name) {
	        this.name = name;
	    }
	    
	    public String toString() {
	        return name;
	    }
	}
	
	@Element(required=false)
    private Location referenceHoleLocation = new Location(LengthUnit.Millimeters);

    @Element(required=false)
    private Location lastHoleLocation = new Location(LengthUnit.Millimeters);

    @Element(required=false)
    private Length partPitch = new Length(4, LengthUnit.Millimeters);
    
    @Element(required=false)
    private Length tapeWidth = new Length(8, LengthUnit.Millimeters);
    
    @Attribute(required=false)
    private TapeType tapeType = TapeType.WhitePaper;

    private Length holeDiameter = new Length(1.5, LengthUnit.Millimeters);
    
    private Length holePitch = new Length(4, LengthUnit.Millimeters);
    
    private Length referenceHoleToPartLinear = new Length(2, LengthUnit.Millimeters);
    
    @Attribute
	private int feedCount = 0;
    
    private Length getHoleToPartLateral() {
        Length tapeWidth = this.tapeWidth.convertToUnits(LengthUnit.Millimeters);
        return new Length(tapeWidth.getValue() / 2 - 0.5, LengthUnit.Millimeters);
    }
	
	@Override
	public boolean canFeedToNozzle(Nozzle nozzle) {
	    return true;
	}
	
	@Override
    public Location getPickLocation() throws Exception {
	    // Find the location of the part linearly along the tape
	    Location l = getPointAlongLine(
                referenceHoleLocation, 
                lastHoleLocation, 
                new Length((feedCount - 1) * partPitch.getValue(), partPitch.getUnits()));
	    // Create the offsets that are required to go from a reference hole
	    // to the part in the tape
	    Length x = getHoleToPartLateral().convertToUnits(l.getUnits());
	    Length y = referenceHoleToPartLinear.convertToUnits(l.getUnits());
        Point p = new Point(x.getValue(), y.getValue());
        // Determine the angle that the tape is at
        double angle = getAngleFromPoint(referenceHoleLocation, lastHoleLocation);
        // Rotate the part offsets by the angle to move it into the right
        // coordinate space
        p = Utils2D.rotatePoint(p, angle);
        // And add the offset to the location we calculated previously
        l = l.add(new Location(l.getUnits(), p.x, p.y, 0, 0));
        // Add in the angle of the tape plus the angle of the part in the tape
        // so that the part is picked at the right angle
        l = l.derive(null, null, null, angle + getLocation().getRotation());
        return l;
    }
	
    static public Location getPointAlongLine(Location a, Location b, Length distance) {
        Point vab = b.subtract(a).getXyPoint();
        double lab = a.getLinearDistanceTo(b);
        Point vu = new Point(vab.x / lab, vab.y / lab);
        vu = new Point(vu.x * distance.getValue(), vu.y * distance.getValue());
        return a.add(new Location(a.getUnits(), vu.x, vu.y, 0, 0));
    }
  
    // Stolen from StackOverflow
    static public double getAngleFromPoint(Location firstPoint,
            Location secondPoint) {
        // above 0 to 180 degrees
        if ((secondPoint.getX() > firstPoint.getX())) {
            return (Math.atan2((secondPoint.getX() - firstPoint.getX()),
                    (firstPoint.getY() - secondPoint.getY())) * 180 / Math.PI);
        }
        // above 180 degrees to 360/0
        else if ((secondPoint.getX() < firstPoint.getX())) {
            return 360 - (Math.atan2((firstPoint.getX() - secondPoint.getX()),
                    (firstPoint.getY() - secondPoint.getY())) * 180 / Math.PI);
        }
        return Math.atan2(0, 0);
    }
	
    public void feed(Nozzle nozzle)
			throws Exception {
        feedCount++;
	}
    
	public TapeType getTapeType() {
        return tapeType;
    }

    public void setTapeType(TapeType tapeType) {
        this.tapeType = tapeType;
    }

    public Location getReferenceHoleLocation() {
        return referenceHoleLocation;
    }

    public void setReferenceHoleLocation(Location referenceHoleLocation) {
        this.referenceHoleLocation = referenceHoleLocation;
    }

    public Location getLastHoleLocation() {
        return lastHoleLocation;
    }

    public void setLastHoleLocation(Location lastHoleLocation) {
        this.lastHoleLocation = lastHoleLocation;
    }

    public Length getHoleDiameter() {
        return holeDiameter;
    }

    public void setHoleDiameter(Length holeDiameter) {
        this.holeDiameter = holeDiameter;
    }

    public Length getHolePitch() {
        return holePitch;
    }

    public void setHolePitch(Length holePitch) {
        this.holePitch = holePitch;
    }

    public Length getPartPitch() {
        return partPitch;
    }

    public void setPartPitch(Length partPitch) {
        this.partPitch = partPitch;
    }

    public Length getTapeWidth() {
        return tapeWidth;
    }

    public void setTapeWidth(Length tapeWidth) {
        this.tapeWidth = tapeWidth;
    }

    public int getFeedCount() {
        return feedCount;
    }

    public void setFeedCount(int feedCount) {
        this.feedCount = feedCount;
    }
    
    public Length getReferenceHoleToPartLinear() {
        return referenceHoleToPartLinear;
    }

    public void setReferenceHoleToPartLinear(Length referenceHoleToPartLinear) {
        this.referenceHoleToPartLinear = referenceHoleToPartLinear;
    }

    @Override
	public String toString() {
		return getName();
	}

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceStripFeederConfigurationWizard(this);
    }
    
    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard())
        };
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }  
}
