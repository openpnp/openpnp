This is a list of commands and responses ([regular expressions](https://en.wikipedia.org/wiki/Regular_expression)) available for the GcodeDriver. Despite its name, the GcodeDriver does not only support the common [G-code numerical control (CNC) programming language](https://en.wikipedia.org/wiki/G-code) but _any_ text-line language, provided it uses decimal numbers and is line oriented (single commands and responses ending with carriage-return and/or line-feed). 

When the driver is commanded by OpenPnP to perform an action it looks up the appropriate Gcode, performs [variable substitution](https://github.com/openpnp/openpnp/wiki/GcodeDriver#variable-substitution) and then sends it to the controller.

Commands can contain multiple lines to send to the controller. Each line is sent as a command and the driver usually waits for the `COMMAND_CONFIRM_REGEX` to match before sending the next one. 

___
**Note**: For [[the most common controller firmwares|Motion-Controller-Firmwares]], OpenPnP can automatically configure many commands and regular expressions for you. It will use the [[axes as configured in your machine|Machine-Axes]] to compose it with the right variables. Use the [[Issues and Solutions]] system to get the proposed codes. You can always dismiss/undo them or change/expand them afterwards. 

The following instructions are for manual configuration or tweaking, where the Issues & Solutions system does not help (or not all the way). Or for special controllers that do not speak a common G-code language.
___

Each of the commands and regular expressions can be set in the configuration panel. 

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

**Note**: For [[the most common controller firmwares|Motion-Controller-Firmwares]], OpenPnP can automatically configure this command for you. Use the [[Issues and Solutions]] system.

Sent in response to the home command. Should home the machine and reset the controller's coordinates to the preferred home location.

**Note**: if you are not using the **Axis Letter** mode on the Driver, the axis and rate variables are not available. 

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| Id             | String   | User defined id of the head being homed. |
| Name           | String   | User defined name of the head being homed. |
| X              | Double   | The home X coordinate. |
| Y              | Double   | The home Y coordinate. |
| etc.           |          |   |
| XL             | String   | The axis letter for the X coordinate. |
| YL             | String   | The axis letter for the Y coordinate. |
| etc.           |          |  |
| FeedRate       | Double   | The homing feed-rate in driver units/min. Note: for homing purposes the smallest feed-rate of the axes is taken. |
| Acceleration   | Double   | The homing acceleration in driver units/s². Note: for homing purposes the smallest acceleration of the axes is taken. |
| Jerk           | Double   | The homing jerk in driver units/s³. Note: for homing purposes the smallest jerk of the axes is taken. |

Example:
```
M84 ; Disable steppers, resetting the Z axis
G4P500 ; Wait half a second for the Z axis to settle
G28 X0 Y0 ; Home X and Y
G92 X0 Y0 Z0 E0 ; Reset machine coordinates to zero.
```

### MOVE_TO_COMMAND

**Note**: For [[the most common controller firmwares|Motion-Controller-Firmwares]], OpenPnP can automatically configure this command for you. Use the [[Issues and Solutions]] system. 

This command has special handling for the X, Y, Z and Rotation variables. If the move does not change one of these variables that variable is replaced with the empty string, removing it from the command. This allows Gcode to be sent containing only the components that are being used which is important for some controllers when moving an "extruder" for the C axis (The current extruder is selected by sending a Gcode tool command like 'T1' in the pre-move-command). The end result is that if a move contains only a change in the C axis only the C axis value will be sent.

**Note**: if you are using the **Axis Letter** mode on the Driver, the X, Y, Z etc. are replaced by the proper controller axis letters. 

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| Id             | String   | User defined id of the HeadMountable (Nozzle, Camera, Actuator) being homed. |
| Name           | String   | User defined name of the HeadMountable (Nozzle, Camera, Actuator) being homed. |
| X              | Double   | The calculated X position for the move. |
| Y              | Double   | The calculated Y position for the move. |
| Z              | Double   | The calculated Z position for the move. |
| Rotation       | Double   | The calculated C or Rotation position for the move in deg. |
| FeedRate       | Double   | The calculated feed rate for the move. |
| XF             | Double   | Forced X position for the move, even if there is no change. |
| YF             | Double   | Forced Y position for the move, even if there is no change.  |
| ZF             | Double   | Forced Z position for the move, even if there is no change.  |
| RotationF      | Double   | Forced C or Rotation position for the move in deg, even if there is no change. |
| XIncreasing    | Boolean  | Sent only if the move includes an X component and the value is increasing. |
| XDecreasing    | Boolean  | Sent only if the move includes an X component and the value is decreasing. |
| YIncreasing    | Boolean  | Sent only if the move includes an Y component and the value is increasing. |
| YDecreasing    | Boolean  | Sent only if the move includes an Y component and the value is decreasing. |
| ZIncreasing    | Boolean  | Sent only if the move includes an Z component and the value is increasing. |
| ZDecreasing    | Boolean  | Sent only if the move includes an Z component and the value is decreasing. |
| RotationIncreasing    | Boolean  | Sent only if the move includes an Rotation component and the value is increasing. |
| RotationDecreasing    | Boolean  | Sent only if the move includes an Rotation component and the value is decreasing. |
| FeedRate       | Double   | The feed-rate in driver units/min.  |
| Acceleration   | Double   | The acceleration in driver units/s².  |
| Jerk           | Double   | The jerk in driver units/s³. |
| XL             | String   | The axis letter for the logical X coordinate. |
| YL             | String   | The axis letter for the logical Y coordinate. |
| ZL             | String   | The axis letter for the logical Z coordinate. |
| RotationL      | String   | The axis letter for the logical Rotation coordinate. |

Example:
```
G0 {X:X%.4f} {Y:Y%.4f} {Z:Z%.4f} {Rotation:E%.4f} F{FeedRate:%.0f} ; Send standard Gcode move
```
Make sure to check the axis designators. The rotation axis might be designated "A" instead of "E" in some controllers, so it must read {Rotation:A%.4f} instead.

**NOTE** For OpenPnP 1.0 add an additional line (it must be moved to MOVE_TO_COMPLETE_COMMAND in newer versions):

`M400 ; Wait for moves to complete before returning`

If you need to move in mils or microns see this post on the form:

https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/openpnp/XV44ij3ZKZ0/eUfbsqRdFQAJ

### MOVE_TO_COMPLETE_COMMAND

**Note**: For [[the most common controller firmwares|Motion-Controller-Firmwares]], OpenPnP can automatically configure this command for you. Use the [[Issues and Solutions]] system.

This command is useful in systems that use multiple controllers where it is desirable to have them move simultaneously.  To use it, remove the "M400 ; Wait for moves to complete before returning" from the MOVE_TO_COMMAND and add it to the MOVE_TO_COMPLETE_COMMAND.  Now the G0 portion of the command will be sent to all involved controllers first.  Then M400 will be sent to each controller in turn, starting from the last one, until all moves are complete.

`M400 ; Wait for moves to complete before returning`

### SET_GLOBAL_OFFSETS_COMMAND

**Note**: For [[the most common controller firmwares|Motion-Controller-Firmwares]], OpenPnP can automatically configure this command for you. Use the [[Issues and Solutions]] system.

This command can reset the axis coordinates at the current machine position to new values. This is used after [[Visual Homing]] but also if you use the [rotational axis **Wrap around** feature](https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings-rotational-axis). 

Example:

`G92 {X:X%.4f} {Y:Y%.4f} {Z:Z%.4f} {A:A%.4f} {B:B%.4f} ; reset coordinates in the controller`

### PICK_COMMAND

**NOTE** This command is deprecated in OpenPnP 2.0. Use the [Vacuum Setup](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Vacuum-Setup) instead. The command remains in OpenPnP so you can move it to the actuator.

For OpenPnP 1.0: Sent to indicate that the machine should pick a part. Typically turns on a vacuum pump or solenoid.

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

**NOTE** This command is deprecated in OpenPnP 2.0. Use the [Vacuum Setup](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Vacuum-Setup) instead. The command remains in OpenPnP so you can move it to the actuator.

For OpenPnP 1.0: Sent to indicate that the machine should place a part. Typically turns off a vacuum pump or solenoid. May also trigger an exhaust solenoid or blow off valve.

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

**NOTE** This command is deprecated in OpenPnP 2.0. Use the [Vacuum Setup](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Vacuum-Setup) instead. The command remains in OpenPnP so you can move it to the actuator.

Sent to turn on the vacuum pump before performing a pick.

### PUMP_OFF_COMMAND

**NOTE** This command is deprecated in OpenPnP 2.0. Use the [Vacuum Setup](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Vacuum-Setup) instead. The command remains in OpenPnP so you can move it to the actuator.

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

Example:
```
M104 S{DoubleValue} ; Set a set-point double value.
```

### ACTUATOR_READ_COMMAND

Sent whenever an Actuator's read() method is called. Along with [ACTUATOR_READ_REGEX](https://github.com/openpnp/openpnp/wiki/GcodeDriver#actuator_read_regex) this can be used to read a value from any type of attached sensor or device.

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| Id             | String   | The user defined id of the actuator. |
| Name           | String   | The user defined name of the actuator. |
| Index          | Index    | The user defined index of the actuator. Can be used to specify a register or port number. |
| Value          | Object   | The object that is sent to the actuator. If it's a double, DoubleValue and IntegerValue will be filled in automatically. The actual value sent is whatever this object's toString() method returns. This gives things that use an actuator more internal freedom on what to send through to the actuator. |
| DoubleValue    | Double   | The Double value sent to the actuator. This is typically user defined in the configuration of the device using the actuator. |
| IntegerValue   | Integer | The Double value sent to the actuator after being cast to an Integer. This is typically user defined in the configuration of the device using the actuator. |

### ACTUATOR_READ_WITH_DOUBLE_COMMAND

**NOTE** This command is deprecated in OpenPnP 2.0. Use the [ACTUATOR_READ_COMMAND](https://github.com/openpnp/openpnp/wiki/GcodeDriver_Command-Reference#actuator_read_command) instead. If ACTUATOR_READ_WITH_DOUBLE_COMMAND is defined but ACTUATOR_READ_COMMAND is not, it will migrate automatically. If both are defined, an issue in the "Issues & Solutions" tab will appear. Accept that issue to replace the read command with the "read with double" command. Dismiss it to remove the "read with double" command entirely.

Sent whenever an Actuator's read(double) method is called. Along with [ACTUATOR_READ_REGEX](https://github.com/openpnp/openpnp/wiki/GcodeDriver#actuator_read_regex) this can be used to read a value from any type of attached sensor or device.

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| Id             | String   | The user defined id of the actuator. |
| Name           | String   | The user defined name of the actuator. |
| Index          | Index    | The user defined index of the actuator. Can be used to specify a register or port number. |
| DoubleValue    | Double   | The Double value sent to the actuator. This is typically user defined in the configuration of the device using the actuator. |
| IntegerValue   | Integer | The Double value sent to the actuator after being cast to an Integer. This is typically user defined in the configuration of the device using the actuator. |

### POST_VISION_HOME_COMMAND

**NOTE** This command is deprecated in the newer OpenPnP 2.0 versions. Use the more universal [`SET_GLOBAL_OFFSETS_COMMAND`](#SET_GLOBAL_OFFSETS_COMMAND) instead.

Sent after [visual homing](https://github.com/openpnp/openpnp/wiki/GcodeDriver#visual-homing) is complete to reset the motion controller's coordinates to their home locations. 

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| X              | Double   | The defined X axis home coordinate. Often 0. |
| Y              | Double   | The defined Y axis home coordinate. Often 0. |

Example:
```
G92 {X:X%.4f} {Y:Y%.4f} ; Send G92 to reset the motion controllers coordinates after homing.
```
