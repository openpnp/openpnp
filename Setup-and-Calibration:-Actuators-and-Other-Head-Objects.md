# Actuators and Other Head Objects
Actuators are generic devices you can attach to your machine to do additional tasks not covered by Nozzles and Cameras. For instance, you can use a solenoid with a pin attached to the head to drag a tape forward to feed it. Actuators are an advanced topic and may require some modifications to the software to do what you want.

## Adding Actuators
1. Open the Machine Setup tab.
2. 
    * If you are adding a head mounted actuator, find the head in the tree on the left. Under the head look for Actuators and select it. Head mounted actuators are often attached to devices such as drag feed solenoids and nozzle change tools.
    * If you are adding a machine mounted actuator, find Actuators under the root of the tree and select it. Machine mounted actuators can be used for things like conveyors and lighting.
3. Add an actuator by pressing the green plus button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/general-add.svg).  
2. Select a actuator driver from the provided list and press the "Accept" button. The newly added actuator will show up in the actuators list.
3. Click on the name of the new actuator to open it's properties.

# GcodeDriver

See [actuate-boolean-command](https://github.com/openpnp/openpnp/wiki/GcodeDriver%3A-Command-Reference#actuate_boolean_command)

See [actuate-double-command](https://github.com/openpnp/openpnp/wiki/GcodeDriver%3A-Command-Reference#actuate_double_command)

## Head Offsets
See [Setting Head Offsets](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Nozzle-Setup#head-offsets) for the general process. It is basically the same for Actuators.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Nozzle Setup|Setup and Calibration: Nozzle Setup]] | [[Table of Contents|Setup and Calibration]] | [[Bottom Camera Setup|Setup and Calibration: Bottom Camera Setup]] |