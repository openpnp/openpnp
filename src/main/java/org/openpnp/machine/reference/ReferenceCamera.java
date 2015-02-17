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

package org.openpnp.machine.reference;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.base.AbstractCamera;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ReferenceCamera extends AbstractCamera implements ReferenceHeadMountable {
    protected final static Logger logger = LoggerFactory
            .getLogger(ReferenceCamera.class);
    
    @Element(required=false)
    private Location headOffsets = new Location(LengthUnit.Millimeters);
    
    @Attribute(required=false)
    protected double rotation = 0;
    
    protected ReferenceMachine machine;
    protected ReferenceDriver driver;

    public ReferenceCamera() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration)
                    throws Exception {
                machine = (ReferenceMachine) configuration.getMachine();
                driver = machine.getDriver();
            }
        });
    }
    
    @Override
    public Location getHeadOffsets() {
        return headOffsets;
    }
   
    @Override
    public void setHeadOffsets(Location headOffsets) {
        this.headOffsets = headOffsets;
    }
    
    @Override
    public void moveTo(Location location, double speed) throws Exception {
        logger.debug("moveTo({}, {})", new Object[] { location, speed } );
        driver.moveTo(this, location, speed);
        machine.fireMachineHeadActivity(head);
    }

    @Override
    public void moveToSafeZ(double speed) throws Exception {
        logger.debug("moveToSafeZ({})", new Object[] { speed } );
        Location l = new Location(getLocation().getUnits(), Double.NaN,
                Double.NaN, 0, Double.NaN);
        driver.moveTo(this, l, speed);
        machine.fireMachineHeadActivity(head);
    }
    
    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    protected BufferedImage applyRotation(BufferedImage image) {
        if (rotation == 0) {
            return image;
        }
        
        // TODO: This should just rotate in place, not change the size of the image. 
        // Just let it cut off.
        
        // Create a rotation transform to determine how big the resulting
        // rotated image should be.
        AffineTransform xform = new AffineTransform();
        xform.rotate(Math.toRadians(-rotation));
        Rectangle2D r2d = xform.createTransformedShape(new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight())).getBounds2D();
        int width = (int) r2d.getWidth();
        int height = (int) r2d.getHeight();
        BufferedImage out = new BufferedImage(width, height, image.getType());
        Graphics2D g2d = out.createGraphics();
        // Create the transform we'll actually use to rotate the image.
        xform = new AffineTransform();
        // Translate the source to the center of the output.
        xform.translate(out.getWidth() / 2, out.getHeight() / 2);
        // Rotate the image.
        xform.rotate(Math.toRadians(-rotation));
        // Translate the image to it's center so the rotation happens about
        // the centerpoint.
        xform.translate(-image.getWidth() / 2, -image.getHeight() / 2);
        g2d.drawImage(image, xform, null);
        g2d.dispose();
        return out;
    }

    @Override
    public Location getLocation() {
        // If this is a fixed camera we just treat the head offsets as it's
        // table location.
        // TODO: This is prety confusing and should be cleaned up and
        // clarified.
        if (getHead() == null) {
            return getHeadOffsets();
        }
        return driver.getLocation(this);
    }
}
