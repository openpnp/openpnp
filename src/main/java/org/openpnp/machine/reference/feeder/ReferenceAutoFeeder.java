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
import org.openpnp.machine.reference.FeederWithOptions;
import org.openpnp.machine.reference.feeder.wizards.ReferenceAutoFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.util.MovableUtils;
import org.pmw.tinylog.Logger;
import org.python.apache.commons.compress.compressors.snappy.FramedSnappyDialect;
import org.simpleframework.xml.Attribute;

public class ReferenceAutoFeeder extends FeederWithOptions {
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

    @Attribute(required = false)
    protected boolean recycleSupport = false;

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
        if (getFeedOptions() != FeedOptions.Normal) {
            if (getFeedOptions() == FeedOptions.SkipNext) {
                setFeedOptions(FeedOptions.Normal);
            }
            return;
        }
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
            throw new Exception("Post pick failed. Unable to find an actuator named " + postPickActuatorName);
        }
        // Note by using the Object generic method, the value will be properly interpreted according to actuator.valueType.
        actuator.actuate((Object)postPickActuatorValue);
    }

    @Override
    public boolean canTakeBackPart() {
        /* in case SkipNext is supposed part already ready at location so we cannot recycle part over it.
           If feed is disabled then recycle is fully on user decision
        */
        return (recycleSupport && getFeedOptions() != FeedOptions.SkipNext);
    }

    @Override
    public void takeBackPart(Nozzle nozzle) throws Exception {
        // first check if we can and want to take back this part (should be always be checked before calling, but to be sure)
        if (nozzle.getPart() == null) {
            throw new UnsupportedOperationException("No part loaded that could be taken back.");
        }
        if (!nozzle.getPart().equals(getPart())) {
            throw new UnsupportedOperationException("Feeder: " + getName() + " - Can not take back " + nozzle.getPart().getId() + " this feeder only supports " + getPart().getId());
        }
        if (!canTakeBackPart()) {
            throw new UnsupportedOperationException("Feeder: " + getName() + " - Currently no free slot or recycle disabled. Can not take back the part.");
        }

        // ok, now put the part back on the location of the last pick
        nozzle.moveToPickLocation(this);
        nozzle.place();
        nozzle.moveToSafeZ();
        if (nozzle.isPartOffEnabled(Nozzle.PartOffStep.AfterPlace) && !nozzle.isPartOff()) {
            throw new Exception("Feeder: " + getName() + " - Putting part back failed, check nozzle tip");
        }
        feedOptions = FeedOptions.SkipNext;
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

    public boolean getRecycleSupport() {
        return recycleSupport;
    }

    public void setRecycleSupport(boolean recycleSupport) {
        this.recycleSupport = recycleSupport;
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
