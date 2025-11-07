package org.openpnp.machine.reference;
import java.util.ArrayList;
import java.util.List;
import org.openpnp.spi.CameraBatchOperation;
import org.openpnp.spi.Camera;
import org.pmw.tinylog.Logger;


// This class is used to keep track of ReferenceMachine using one or more cameras to
// take several images in a batch. It lets the camera defer having its light turned off
// until the end of the batch of operations.
public class ReferenceCameraBatchOperation implements CameraBatchOperation {

    // This is the list of cameras used in this operation, or
    // null if there is no batch operation in progress
    private List<Camera> cameras;
    private int nestingLevel = 0;

    // Start a batch
    public void startBatchOperation(String name) {
        if(cameras==null) {
            cameras = new ArrayList<Camera>();
        }
        nestingLevel += 1;
        Logger.info("Start level {} {}",nestingLevel,name);
    }

    // End the batch operation, and get any cameras used in this operation to turn off their lights.
    public void endBatchOperation(String name) throws Exception {
        Logger.info("End level {} {}",nestingLevel,name);

        nestingLevel -= 1;

        if(nestingLevel==0) {
            List<Camera> camerasFormerlyInUse = cameras;
            cameras = null;
            for(Camera c: camerasFormerlyInUse) {
                Logger.info("Processing camera {}",c);
                c.actuateLightAfterCapture();
            }
        }

        if(nestingLevel<0) {
            Logger.error("underflow");
            nestingLevel = 0;
        }
    }

    public boolean registerWithBatchOperation(Camera c) {
        if(cameras==null) {
            // There is no batch in progress
            return false;
        }

        if(!cameras.contains(c)) {
            Logger.info("Registering camera {}",c);
            cameras.add(c);
        }

        return true;
    }
}
