## What is it?

Z probing allows you to capture a Z coordinate, along with the normal X,Y, and Rotation, when using the capture coordinates with camera function. A Z probe is configured by adding an Actuator that can read a numeric value. Some examples of Z probe hardware are laser displacement sensors, nozzle switches, or deployable sensors.

## What is it _not_?

Z Probing is not to be confused with the [[Contact Probing Nozzle]], which is a completely separate concept. The [[Contact Probing Nozzle]] can be integrated into production, as measurements can take place "on the go" with a part on the nozzle (sandwiching), to get the _real live_ part, solder paste, and board surface heights, or _real live_ feeder pick heights.

Conversely, Z Probing (explained here) is used to measure any Z surfaces. The probing is always manual and intended for setting up the machine and feeders up front. If you have a laser displacement sensor, it may be able to measure delicate surfaces, like lose parts in feeders, without making contact and disturbing them.

Of course, the two facilities can also be combined.

## Usage

To use Z probing:

1. Create a new Actuator to read the probe.
2. Configure the Actuator in the driver so that it performs it's probing action on read, and returns a numeric value in millimeters.
3. Set the Z probe Actuator on the Head in Machine Setup -> Heads -> Configuration under Z Probe Actuator.

Once the Z probe is configured, use it by pressing the Capture Camera Location button when entering a coordinate. OpenPnP will first capture the X, Y, and Rotation coordinates of the camera, and then will move the Z probe to the camera's position and perform a read to capture the Z coordinate.