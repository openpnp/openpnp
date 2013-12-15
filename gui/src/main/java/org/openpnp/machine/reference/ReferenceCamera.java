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

import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.base.AbstractCamera;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ReferenceCamera extends AbstractCamera implements ReferenceHeadMountable {
    private final static Logger logger = LoggerFactory
            .getLogger(ReferenceCamera.class);
    
    @Element
    private Location headOffsets = new Location(LengthUnit.Millimeters);
    
    private ReferenceMachine machine;
    private ReferenceDriver driver;

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
    
    public Location getHeadOffsets() {
        return headOffsets;
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

    @Override
    public Location getLocation() {
        return driver.getLocation(this).add(headOffsets);
    }
}
