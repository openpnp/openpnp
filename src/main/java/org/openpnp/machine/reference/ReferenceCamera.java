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
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;

import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
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
    
    @Attribute(required=false)
    protected boolean flipX = false;
    
    @Attribute(required=false)
    protected boolean flipY = false;
    
    @Element(required=false)
    protected Length safeZ = new Length(0, LengthUnit.Millimeters);

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
        logger.debug("{}.moveToSafeZ({})", new Object[] { getName(), speed } );
        Length safeZ = this.safeZ.convertToUnits(getLocation().getUnits());
        Location l = new Location(getLocation().getUnits(), Double.NaN,
                Double.NaN, safeZ.getValue(), Double.NaN);
        driver.moveTo(this, l, speed);
        machine.fireMachineHeadActivity(head);
    }
    
    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }
    
    public boolean isFlipX() {
        return flipX;
    }

    public void setFlipX(boolean flipX) {
        this.flipX = flipX;
    }

    public boolean isFlipY() {
        return flipY;
    }

    public void setFlipY(boolean flipY) {
        this.flipY = flipY;
    }

    protected BufferedImage transformImage(BufferedImage image) {
        if (rotation == 0 && !flipX && !flipY) {
            return image;
        }
        
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g2d = out.createGraphics();
        AffineTransform xform = new AffineTransform();

        if (flipY) {
            xform.scale(-1, 1); 
            xform.translate(-image.getWidth(), 0);
        }
        
        if (flipX) {
            xform.scale(1, -1); 
            xform.translate(0, -image.getHeight());
        }
        
        if (rotation != 0) {
            xform.rotate(Math.toRadians(-rotation), image.getWidth() / 2, image.getHeight() / 2);
        }
        
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

	public Length getSafeZ() {
		return safeZ;
	}

	public void setSafeZ(Length safeZ) {
		this.safeZ = safeZ;
	}

    @Override
    public void close() throws IOException {
    }
}
