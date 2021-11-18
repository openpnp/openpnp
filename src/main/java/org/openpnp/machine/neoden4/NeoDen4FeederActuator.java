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

package org.openpnp.machine.neoden4;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.neoden4.wizards.NeoDen4FeederActuatorConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Driver;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.PropertySheetHolder.PropertySheet;
import org.openpnp.spi.base.AbstractActuator;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class NeoDen4FeederActuator extends ReferenceActuator{

	@Attribute(required = true)
    private int feederId = 1;
	
    @Attribute(required = true)
    private int peelerId = 1;
    
    @Attribute(required = true)
    private int feedStrength = 50;
    
    @Attribute(required = true)
    private int peelStrength = 30;
    
    @Attribute(required = true) 
    private int peelLength = 100; // 100% -> 5*feedLength
    
    
    public NeoDen4FeederActuator() {
    }
	
    @Override
    public Wizard getConfigurationWizard() {
        return new NeoDen4FeederActuatorConfigurationWizard(getMachine(), this);
    }
	
    protected ReferenceMachine getMachine() {
    	return (ReferenceMachine) Configuration.get().getMachine();
    }
	
	@Override
	public void actuate(double feedLength) throws Exception {
		if (feedLength > 0) {
			Logger.debug("{}.actuate({})", getName(), feedLength);
			NeoDen4Driver driver = (NeoDen4Driver) getDriver();
			driver.feed(feederId, feedStrength, (int) feedLength);
			driver.peel(peelerId, peelStrength, (int) ((peelLength / 100.0) * 5 * feedLength));
		} else {
			Logger.error("Actuation feedLength can't be lower than 0!");
		}
	}

    protected void driveActuation(double value) throws Exception {
        getDriver().actuate(this, value);
    }
    
 
  public int getFeederId() {
      return this.feederId;
  }

  public void setFeederId(int feederId) {
      this.feederId = feederId;
  }
      
  public int getPeelerId() {
      return this.peelerId;
  }

  public void setPeelerId(int peelerId) {
      this.peelerId = peelerId;
  }
  
  public int getFeedStrength() {
      return this.feedStrength;
  }

  public void setFeedStrength(int feedStrength) {
      this.feedStrength = feedStrength;
  }
  
  public int getPeelStrength() {
      return this.peelStrength;
  }

  public void setPeelStrength(int peelerStrength) {
      this.peelStrength = peelerStrength;
  }
  
//  public int getFeedLength() {
//      return this.feedLength;
//  }
//
//  public void setFeedLength(int feedLength) {
//      this.feedLength = feedLength;
//  }
  
  public int getPeelLength() {
      return this.peelLength;
  }

  public void setPeelLength(int peelLength) {
      this.peelLength = peelLength;
  }
  
  @Override
  public void moveTo(Location location, MotionOption... options) throws Exception{
	  
  }

}