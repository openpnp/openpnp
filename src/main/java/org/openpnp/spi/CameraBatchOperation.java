package org.openpnp.spi;

public interface CameraBatchOperation {

    // Start a batch of camera operations
    // The 'name' parameter is to identify the subsystem which is starting the batch.
    // This is just for diagnostic purposes - to aid debugging when a subystem neglects
    // to close its batch.
    public void startBatchOperation(String name);

    // End a batch of camera operations. This must be paired with a call to start...
    public void endBatchOperation(String name) throws Exception;

    // Return true if there is a batch of camera capture operations in progress and
    // the camera can be registered to that batch.
    public boolean registerWithBatchOperation(Camera c);

}
