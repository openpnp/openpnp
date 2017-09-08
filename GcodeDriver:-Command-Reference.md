This is a list of Gcode configuration options available for the GcodeDriver. Each of these options can be specified in the `machine.xml` or in the configuration panel. When the driver is commanded by OpenPnP to perform an action it looks up the appropriate Gcode, performs variable substitution and then sends it to the controller.

Commands can contain multiple lines to send to the controller. Each line is sent as a command and the driver waits for the COMMAND_CONFIRM_REGEX to match before sending the next one.

### CONNECT_COMMAND

Sent after the driver finishes connecting to the serial port. Can be used to send any initialization parameters the controller needs.

Example:
```
G21 ; Set millimeters mode
G90 ; Set absolute positioning mode
M82 ; Set absolute mode for extruder
```

### ENABLE_COMMAND

Sent when the machine is enabled, primarily when the big START button is pressed. Can be used to turn on motors and lighting, start pumps, reset solenoids, etc.

Example:
```
M810 ; Turn on LED lighting
```

### DISABLE_COMMAND

Sent when the machine is disabled, primarily when the big STOP button is pressed or before shutting down. Should turn off everything.

Example:
```
M84 ; Disable steppers
M811 ; Turn off LED lighting
```

### HOME_COMMAND

Sent in response to the home command. Should home the machine and reset the controller's coordinates to the preferred home location.

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| Id             | String   | User defined id of the head being homed. |
| Name           | String   | User defined name of the head being homed. |

Example:
```
M84 ; Disable steppers, resetting the Z axis
G4P500 ; Wait half a second for the Z axis to settle
G28 X0 Y0 ; Home X and Y
G92 X0 Y0 Z0 E0 ; Reset machine coordinates to zero.
```

### MOVE_TO_COMMAND

This command has special handling for the X, Y, Z and Rotation variables. If the move does not change one of these variables that variable is replaced with the empty string, removing it from the command. This allows Gcode to be sent containing only the components that are being used which is important for some controllers when moving an "extruder" for the C axis. The end result is that if a move contains only a change in the C axis only the C axis value will be sent.

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| Id             | String   | User defined id of the HeadMountable (Nozzle, Camera, Actuator) being homed. |
| Name           | String   | User defined name of the HeadMountable (Nozzle, Camera, Actuator) being homed. |
| X              | Double   | The calculated X position for the move. |
| Y              | Double   | The calculated Y position for the move. |
| Z              | Double   | The calculated Z position for the move. |
| Rotation       | Double   | The calculated C or Rotation position for the move in deg. |
| FeedRate       | Double   | The calculated feed rate for the move. |

Example:
```
G0 {X:X%.4f} {Y:Y%.4f} {Z:Z%.4f} {Rotation:E%.4f} F{FeedRate:%.0f} ; Send standard Gcode move
M400 ; Wait for moves to complete before returning
```

If you need to move in mils or microns see this post on the form:

https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/openpnp/XV44ij3ZKZ0/eUfbsqRdFQAJ

### PICK_COMMAND

Sent to indicate that the machine should pick a part. Typically turns on a vacuum pump or solenoid.

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| Id             | String   | The user defined id of the nozzle. |
| Name           | String   | The user defined name of the nozzle. |

Example:
```
M808 ; Turn on pump
M800 ; Turn on nozzle 1 vacuum solenoid
```

### PLACE_COMMAND

Sent to indicate that the machine should place a part. Typically turns off a vacuum pump or solenoid. May also trigger an exhaust solenoid or blow off valve.

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| Id             | String   | The user defined id of the nozzle. |
| Name           | String   | The user defined name of the nozzle. |

Example:
```
M809 ; Turn off pump
M801 ; Turn off nozzle 1 vacuum solenoid
M802 ; Turn on nozzle 1 exhaust solenoid
G4P250 ; Wait 250 milliseconds
M803 ; Turn off nozzle 1 exhaust solenoid
```

### PUMP_ON_COMMAND

Sent to turn on the vacuum pump before performing a pick.

### PUMP_OFF_COMMAND

Sent to turn off the vacuum pump after a place if there are no longer any nozzles that are picked.

### ACTUATE_BOOLEAN_COMMAND

Sent whenever an Actuator's actuate(boolean) method is called. This is currently used by the ReferenceDragFeeder to fire a drag solenoid. Actuators are generally an area where people customize their machines, so this is here to support customizations such as automated feeders.

The `True` and `False` variables can be used to substitute any string for either a true or false value. By specifying a value as the format string and using both the `True` and `False` variables you can choose what will be sent in either case. See the example for details.

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| Id             | String   | The user defined id of the actuator. |
| Name           | String   | The user defined name of the actuator. |
| Index          | Index    | The user defined index of the actuator. Can be used to specify a register or port number. |
| BooleanValue   | Boolean  | A Boolean representing whether the actuator was turned on or off. |
| True           | Boolean  | Boolean true if the actuator is turned on, or null if it's turned off. This can be used to include a string only when the value is true. See the example for details. |
| False          | Boolean  | Boolean false if the actuator is turned off, or null if it's turned on. This can be used to include a string only when the value is false. See the example for details. |

Example:
```
M800 P{True:1}{False:0} ; Send "M800 P1" if the actuator is turned on, or "M800 P0" if the actuator is turned off.
```

### ACTUATE_DOUBLE_COMMAND

Sent whenever an Actuator's actuate(double) method is called. This is currently used by the ReferenceAutoFeeder to trigger a feed operation. Actuators are generally an area where people customize their machines, so this is here to support customizations such as automated feeders.

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| Id             | String   | The user defined id of the actuator. |
| Name           | String   | The user defined name of the actuator. |
| Index          | Index    | The user defined index of the actuator. Can be used to specify a register or port number. |
| DoubleValue    | Double   | The Double value sent to the actuator. This is typically user defined in the configuration of the device using the actuator. |
| IntegerValue   | Integer | The Double value sent to the actuator after being cast to an Integer. This is typically user defined in the configuration of the device using the actuator. |

### ACTUATOR_READ_COMMAND

Sent whenever an Actuator's read() method is called. Along with ACTUATOR_READ_REGEX this can be used to read a value from any type of attached sensor or device.

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| Id             | String   | The user defined id of the actuator. |
| Name           | String   | The user defined name of the actuator. |
| Index          | Index    | The user defined index of the actuator. Can be used to specify a register or port number. |

### POST_VISION_HOME_COMMAND

Sent after [visual homing](https://github.com/openpnp/openpnp/wiki/GcodeDriver#visual-homing) is complete to reset the motion controller's coordinates to their home locations. 

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| X              | Double   | The defined X axis home coordinate. Often 0. |
| Y              | Double   | The defined Y axis home coordinate. Often 0. |

Example:
```
G92 {X:X%.4f} {Y:Y%.4f} ; Send G92 to reset the motion controllers coordinates after homing.
```
