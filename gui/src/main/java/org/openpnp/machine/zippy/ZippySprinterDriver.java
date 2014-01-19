/*
 	Copyright (C) 2013 Richard Spelling <openpnp@chebacco.com>
 	
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

package org.openpnp.machine.zippy;

import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.driver.SprinterDriver;
import org.simpleframework.xml.Attribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ZippySprinterDriver extends SprinterDriver {

    @Attribute(required=false) 
    private int vacpumpPin;

    @Attribute(required=false) 
    private boolean invertVacpump;

	
  //  @Override
  public void vac_toggle() throws Exception {
	    


		boolean vac_is_on = false;
		
		
	  
		if(vac_is_on){
              //if on then turn off
          sendCommand(String.format("M42 P%d S%d", vacpumpPin, invertVacpump ? 255 : 0));
          vac_is_on = false;
      } else {
              //if off then turn on
              sendCommand(String.format("M42 P%d S%d", vacpumpPin, invertVacpump ? 0 : 255));
          vac_is_on = true;
      }
      dwell();
  }

}
