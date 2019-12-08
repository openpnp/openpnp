package org.openpnp.vision.pipeline.stages;


import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Head;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Stage;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

@Stage(description="Performs simple actuator write. Machine must be connected, otherwise error is thrown.")
public class ActuatorWrite extends CvStage {
	
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
    

    @Override
    public Result process(CvPipeline pipeline) throws Exception {

    	if (actuatorName == null || actuatorName.equals("")) {
            Logger.warn("No actuator name specified for pipeline {}.", getName());
        }
    	else{
    		
	        Actuator actuator =  Configuration.get().getMachine().getActuatorByName(actuatorName);
	        	        
	        if (actuator == null) {	        	
	            for (final Head head : Configuration.get().getMachine().getHeads()) {
	            	actuator = head.getActuatorByName(actuatorName);
	            	if(  actuator != null){
	            		break;
	                }
	            }		        		            
	        }
	        
	        if (actuator == null)
	        {
	        	throw new Exception("Actuator writing (CvStage operation) failed. Unable to find an actuator named " + actuatorName);
	        }
	        else{
	        		        
		        if (actuatorType == ActuatorType.Boolean) {
		            actuator.actuate(actuatorValue != 0);
		        }
		        else {
		            actuator.actuate(actuatorValue);
		        }
	        }
    	}	
        
        return null;
    }
}
