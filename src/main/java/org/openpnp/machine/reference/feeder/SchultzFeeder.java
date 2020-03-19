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

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.SchultzFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

public class SchultzFeeder extends ReferenceFeeder {
    public enum ActuatorType {
        Double,
        Boolean
    }
    
    @Attribute(required=false)
    protected String actuatorName;
    
    @Attribute(required=false)
    protected ActuatorType actuatorType = ActuatorType.Double;
    
    @Attribute(required=false)
    protected double actuatorValue;

    @Attribute(required=false)
    protected String postPickActuatorName;
    
    @Attribute(required=false)
    protected ActuatorType postPickActuatorType = ActuatorType.Double;
    
    @Attribute(required=false)
    protected String feedCountActuatorName;
    
    @Attribute(required=false)
    protected ActuatorType feedCountActuatorType = ActuatorType.Double;
    
    @Attribute(required=false)
    protected String clearCountActuatorName;
    
    @Attribute(required=false)
    protected ActuatorType clearCountActuatorType = ActuatorType.Double;
    
    @Attribute(required=false)
    protected String pitchActuatorName;
    
    @Attribute(required=false)
    protected ActuatorType pitchActuatorType = ActuatorType.Double;
    
    @Attribute(required=false)
    protected String togglePitchActuatorName;
    
    @Attribute(required=false)
    protected ActuatorType togglePitchActuatorType = ActuatorType.Double;
    
    @Attribute(required=false)
    protected String statusActuatorName;
    
    @Attribute(required=false)
    protected ActuatorType statusActuatorType = ActuatorType.Double;
    
    @Attribute(required=false)
    protected String idActuatorName;
    
    @Attribute(required=false)
    protected ActuatorType idActuatorType = ActuatorType.Double;
    
    @Attribute(required=false)
    protected String fiducialPart;
    
    @Override
    public boolean isParentIdChangable() {
        return false;
    }
    
    @Override
    public Location getPickLocation() throws Exception {
        return getLocation();
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
        if (actuatorType == ActuatorType.Boolean) {
            actuator.actuate(actuatorValue != 0);
        }
        else {
            actuator.actuate(actuatorValue);
        }
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
        
        if (postPickActuatorType == ActuatorType.Boolean) {
            actuator.actuate(actuatorValue != 0);
        }
        else {
            actuator.actuate(actuatorValue);
        }
    }
    
    public String getActuatorName() {
        return actuatorName;
    }

    public void setActuatorName(String actuatorName) {
        this.actuatorName = actuatorName;
    }

    public ActuatorType getActuatorType() {
        return actuatorType;
    }

    public void setActuatorType(ActuatorType actuatorType) {
        this.actuatorType = actuatorType;
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

    public ActuatorType getPostPickActuatorType() {
        return postPickActuatorType;
    }

    public void setPostPickActuatorType(ActuatorType postPickActuatorType) {
        this.postPickActuatorType = postPickActuatorType;
    }

    public String getFeedCountActuatorName() {
        return feedCountActuatorName;
    }

    public void setFeedCountActuatorName(String feedCountActuatorName) {
        this.feedCountActuatorName = feedCountActuatorName;
    }

    public ActuatorType getFeedCountActuatorType() {
        return feedCountActuatorType;
    }

    public void setFeedCountActuatorType(ActuatorType feedCountActuatorType) {
        this.feedCountActuatorType = feedCountActuatorType;
    }

    public String getClearCountActuatorName() {
        return clearCountActuatorName;
    }

    public void setClearCountActuatorName(String clearCountActuatorName) {
        this.clearCountActuatorName = clearCountActuatorName;
    }

    public ActuatorType getClearCountActuatorType() {
        return clearCountActuatorType;
    }

    public void setClearCountActuatorType(ActuatorType clearCountActuatorType) {
        this.clearCountActuatorType = clearCountActuatorType;
    }

    public String getPitchActuatorName() {
        return pitchActuatorName;
    }

    public void setPitchActuatorName(String pitchActuatorName) {
        this.pitchActuatorName = pitchActuatorName;
    }

    public ActuatorType getPitchActuatorType() {
        return pitchActuatorType;
    }

    public void setPitchActuatorType(ActuatorType pitchActuatorType) {
        this.pitchActuatorType = pitchActuatorType;
    }

    public String getTogglePitchActuatorName() {
        return togglePitchActuatorName;
    }

    public void setTogglePitchActuatorName(String togglePitchActuatorName) {
        this.togglePitchActuatorName = togglePitchActuatorName;
    }

    public ActuatorType getTogglePitchActuatorType() {
        return togglePitchActuatorType;
    }

    public void setTogglePitchActuatorType(ActuatorType togglePitchActuatorType) {
        this.togglePitchActuatorType = togglePitchActuatorType;
    }

    public String getStatusActuatorName() {
        return statusActuatorName;
    }

    public void setStatusActuatorName(String statusActuatorName) {
        this.statusActuatorName = statusActuatorName;
    }

    public ActuatorType getStatusActuatorType() {
        return statusActuatorType;
    }

    public void setStatusActuatorType(ActuatorType statusActuatorType) {
        this.statusActuatorType = statusActuatorType;
    }

    public String getIdActuatorName() {
        return idActuatorName;
    }

    public void setIdActuatorName(String idActuatorName) {
        this.idActuatorName = idActuatorName;
    }

    public ActuatorType getIdActuatorType() {
        return idActuatorType;
    }

    public void setIdActuatorType(ActuatorType idActuatorType) {
        this.idActuatorType = idActuatorType;
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
