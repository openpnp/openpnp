package org.openpnp.machine.zippy;

import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.Utils2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZippyNozzle extends ReferenceNozzle {
    private final static Logger logger = LoggerFactory
            .getLogger(ZippyNozzle.class);
    private NozzleTip currentNozzleTip = nozzletips.get(0); //work with only first one till we write changer code
    
    @Override
    public void moveTo(Location location, double speed) throws Exception {
    	//pull offsets from current nozzle tip
    	Location offset = ((ZippyNozzleTip) currentNozzleTip).getNozzleOffsets();

    	// Create the point that represents the nozzle tip offsets (offset always for angle zero)
		Point p = new Point(offset.getX(), 	offset.getY());
    	
    	// Rotate and translate the point into the same rotational coordinate space as the new location
		p = Utils2D.rotatePoint(p, location.getRotation());

		// Update the  offset Location with the transformed point
		offset = offset.derive(p.getX(), p.getY(), null, null);
		
		//subtract rotated offset 
    	Location adjustedLocation = location.subtract(offset);
    	
    	//log calculated offsets
        logger.debug("{}.moveTo(adjusted,{}, original,{},  {})", new Object[] { id, adjustedLocation, location, speed } );
        
        //call super to move to corrected position
    	super.moveTo(adjustedLocation, speed);
    }
    @Override
    public NozzleTip getNozzleTip() {
        return currentNozzleTip;
    }

    @Override
    public boolean canPickAndPlace(Feeder feeder, Location placeLocation) {
		boolean result = currentNozzleTip.canHandle(feeder.getPart());
		logger.debug("{}.canPickAndPlace({},{}) => {}", new Object[]{getId(), feeder, placeLocation, result});
    	return result;
	}

    //    @Override
    public void addNozzleTip(NozzleTip nozzletip) throws Exception {
        nozzletips.add(nozzletip);
   }

}
