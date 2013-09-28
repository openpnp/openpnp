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
