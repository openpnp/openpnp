/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.feeder;
import org.I18n.I18n;

import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferenceAutoFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.util.MovableUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

public class ReferenceAutoFeeder extends ReferenceFeeder {
    @Attribute(required=false)
    protected String actuatorName;
    
    @Deprecated
    @Attribute(required=false)
    protected Actuator.ActuatorValueType actuatorType = null;
    
    @Attribute(required=false)
    protected double actuatorValue;

    @Attribute(required=false)
    protected String postPickActuatorName;
    
    @Deprecated
    @Attribute(required=false)
    protected Actuator.ActuatorValueType postPickActuatorType = null;
    
    @Attribute(required=false)
    protected double postPickActuatorValue;
    
    @Attribute(required=false)
    protected boolean moveBeforeFeed;

    @Override
    public Location getPickLocation() throws Exception {
        return location;
    }

    public ReferenceAutoFeeder() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                // Migrate actuator value types.
                if (actuatorType != null) { 
                    Actuator actuator = Configuration.get().getMachine().getActuatorByName(actuatorName);
                    AbstractActuator.suggestValueType(actuator, actuatorType);
                    actuatorType = null;
                }
                if (postPickActuatorType != null) {
                    Actuator actuator = Configuration.get().getMachine().getActuatorByName(postPickActuatorName);
                    AbstractActuator.suggestValueType(actuator, postPickActuatorType);
                    postPickActuatorType = null;
                }
            }
        });
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        if (actuatorName == null || actuatorName.equals("")) {
            Logger.warn("No actuatorName specified for feeder {}.", getName());
            return;
        }
        Actuator actuator = nozzle.getHead().getActuatorByName(actuatorName);
        if (actuator == null) {
            actuator = Configuration.get().getMachine().getActuatorByName(actuatorName);
        }
        if (actuator == null) {
            throw new Exception(I18n.gettext("Feed failed. Unable to find an actuator named ") + actuatorName);
        }
        if (isMoveBeforeFeed()) {
            MovableUtils.moveToLocationAtSafeZ(nozzle, getPickLocation().derive(null, null, Double.NaN, null));
        }
        // Note by using the Object generic method, the value will be properly interpreted according to actuator.valueType.
        actuator.actuate((Object)actuatorValue);
    }
    
    @Override
    public void postPick(Nozzle nozzle) throws Exception {
        if (postPickActuatorName == null || postPickActuatorName.equals("")) {
            return;
        }
        Actuator actuator = nozzle.getHead().getActuatorByName(postPickActuatorName);
        if (actuator == null) {
            actuator = Configuration.get().getMachine().getActuatorByName(postPickActuatorName);
        }
        if (actuator == null) {
            throw new Exception(I18n.gettext("Post pick failed. Unable to find an actuator named ") + postPickActuatorName);
        }
        // Note by using the Object generic method, the value will be properly interpreted according to actuator.valueType.
        actuator.actuate((Object)postPickActuatorValue);
    }
    
    public String getActuatorName() {
        return actuatorName;
    }

    public void setActuatorName(String actuatorName) {
        this.actuatorName = actuatorName;
    }

    public double getActuatorValue() {
        return actuatorValue;
    }

    public void setActuatorValue(double actuatorValue) {
        this.actuatorValue = actuatorValue;
    }

    public String getPostPickActuatorName() {
        return postPickActuatorName;
    }

    public void setPostPickActuatorName(String postPickActuatorName) {
        this.postPickActuatorName = postPickActuatorName;
    }

    public double getPostPickActuatorValue() {
        return postPickActuatorValue;
    }

    public void setPostPickActuatorValue(double postPickActuatorValue) {
        this.postPickActuatorValue = postPickActuatorValue;
    }

    public boolean isMoveBeforeFeed() {
		return moveBeforeFeed;
	}

	public void setMoveBeforeFeed(boolean moveBeforeFeed) {
		this.moveBeforeFeed = moveBeforeFeed;
	}

	@Override
    public Wizard getConfigurationWizard() {
        return new ReferenceAutoFeederConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }
}
