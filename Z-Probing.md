Z probing allows you to capture a Z coordinate, along with the normal X,Y, and Rotation, when using the capture coordinates with camera function. A Z probe is configured by adding an Actuator that can read a numeric value. Some examples of Z probe hardware are laser displacement sensors, nozzle switches, or deployable sensors.

To use Z probing:

1. Create a new Actuator to read the probe.
2. Configure the Actuator in the driver so that it performs it's probing action on read, and returns a numeric value in millimeters.
3. Set the Z probe Actuator on the Head in Machine Setup -> Heads -> Configuration under Z Probe Actuator.

Once the Z probe is configured, use it by pressing the Capture Camera Location button when entering a coordinate. OpenPnP will first capture the X, Y, and Rotation coordinates of the camera, and then will move the Z probe to the camera's position and perform a read to capture the Z coordinate.