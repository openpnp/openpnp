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

import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.SchultzFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.util.MovableUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

public class SchultzFeeder extends ReferenceFeeder {
    @Attribute(required=false)
    protected String actuatorName;
    
    @Deprecated
    @Attribute(required=false)
    protected Class<?> actuatorType = null;
    
    @Attribute(required=false)
    protected double actuatorValue;

    @Attribute(required=false)
    protected String postPickActuatorName;
    
    @Deprecated
    @Attribute(required=false)
    protected Class<?> postPickActuatorType = null;
    
    @Attribute(required=false)
    protected String feedCountActuatorName;
    
    @Deprecated
    @Attribute(required=false)
    protected Class<?> feedCountActuatorType = null;
    
    @Attribute(required=false)
    protected String clearCountActuatorName;
    
    @Deprecated
    @Attribute(required=false)
    protected Class<?> clearCountActuatorType = null;
    
    @Attribute(required=false)
    protected String pitchActuatorName;
    
    @Deprecated
    @Attribute(required=false)
    protected Class<?> pitchActuatorType = null;
    
    @Attribute(required=false)
    protected String togglePitchActuatorName;
    
    @Deprecated
    @Attribute(required=false)
    protected Class<?> togglePitchActuatorType = null;
    
    @Attribute(required=false)
    protected String statusActuatorName;
    
    @Deprecated
    @Attribute(required=false)
    protected Class<?> statusActuatorType = null;
    
    @Attribute(required=false)
    protected String idActuatorName;
    
    @Deprecated
    @Attribute(required=false)
    protected Class<?> idActuatorType = null;
    
    @Attribute(required=false)
    protected String fiducialPart;
    
    @Override
    public Location getPickLocation() throws Exception {
        return location;
    }

    public SchultzFeeder() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                // Migrate actuator value types.
                if (actuatorType != null) { 
                    Actuator actuator = Configuration.get().getMachine().getActuatorByName(actuatorName);
                    AbstractActuator.suggestValueClass(actuator, Double.class);
                    actuatorType = null;
                }
                if (postPickActuatorType != null) {
                    Actuator actuator = Configuration.get().getMachine().getActuatorByName(postPickActuatorName);
                    AbstractActuator.suggestValueClass(actuator, Double.class);
                    postPickActuatorType = null;
                }
                if (clearCountActuatorType != null) {
                    Actuator actuator = Configuration.get().getMachine().getActuatorByName(clearCountActuatorName);
                    AbstractActuator.suggestValueClass(actuator, Double.class);
                    clearCountActuatorType = null;
                }
                if (togglePitchActuatorType != null) {
                    Actuator actuator = Configuration.get().getMachine().getActuatorByName(togglePitchActuatorName);
                    AbstractActuator.suggestValueClass(actuator, Double.class);
                    togglePitchActuatorType = null;
                }
                // NOTE: all the other actuator types are actually read only, no need to migrate.
                feedCountActuatorType = null;
                pitchActuatorType = null;
                statusActuatorType = null;
                idActuatorType = null;
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
            throw new Exception("Feed failed. Unable to find an actuator named " + actuatorName);
        }
        MovableUtils.moveToLocationAtSafeZ(nozzle, getPickLocation().derive(null, null, Double.NaN, null));
        AbstractActuator.suggestValueClass(actuator, Double.class);
        actuator.actuate(actuatorValue);
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
            throw new Exception("Post pick failed. Unable to find an actuator named " + postPickActuatorName);
        }
        // TODO: check status before feed?
        
        AbstractActuator.suggestValueClass(actuator, Double.class);
        actuator.actuate(actuatorValue);
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

    public String getFeedCountActuatorName() {
        return feedCountActuatorName;
    }

    public void setFeedCountActuatorName(String feedCountActuatorName) {
        this.feedCountActuatorName = feedCountActuatorName;
    }

    public String getClearCountActuatorName() {
        return clearCountActuatorName;
    }

    public void setClearCountActuatorName(String clearCountActuatorName) {
        this.clearCountActuatorName = clearCountActuatorName;
    }

    public String getPitchActuatorName() {
        return pitchActuatorName;
    }

    public void setPitchActuatorName(String pitchActuatorName) {
        this.pitchActuatorName = pitchActuatorName;
    }

    public String getTogglePitchActuatorName() {
        return togglePitchActuatorName;
    }

    public void setTogglePitchActuatorName(String togglePitchActuatorName) {
        this.togglePitchActuatorName = togglePitchActuatorName;
    }

    public String getStatusActuatorName() {
        return statusActuatorName;
    }

    public void setStatusActuatorName(String statusActuatorName) {
        this.statusActuatorName = statusActuatorName;
    }

    public String getIdActuatorName() {
        return idActuatorName;
    }

    public void setIdActuatorName(String idActuatorName) {
        this.idActuatorName = idActuatorName;
    }

    public void setFiducialPart(String part) {
        fiducialPart = part;
    }

    public String getFiducialPart() {
		return fiducialPart;
    }

	@Override
    public Wizard getConfigurationWizard() {
        return new SchultzFeederConfigurationWizard(this);
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
