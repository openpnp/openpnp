GcodeDriver is a universal driver that makes it possible to configure complex machines and add-on hardware such as feeders without having to write any custom driver code. All configuration is done through commands in the configuration files.

GcodeDriver introduces a new concept to OpenPnP drivers: Sub-Drivers. Sub-Drivers allow you to embed multiple drivers in your configuration and each one can control a different device. This is helpful for cases where you might have one board that controls your machine's movements and another that controls your feeders.

Work on this feature is being done in #106: https://github.com/openpnp/openpnp/issues/106

# Controller Specific

Please see the following pages for additional information when configuring GcodeDriver for specific controllers:

* [[Grbl]]
* [[TinyG]]
* [[Marlin]]

# Examples
Please see [[GcodeDriver: Example Configurations]] for some community contributed example configurations. If you find one that matches your controller you can use it as a starting point for your own system.

# Configuration

To configure the GcodeDriver it is necessary to at least set a COMMAND_CONFIRM_REGEX and one or more commands. The COMMAND_CONFIRM_REGEX is a regular expression that the driver will use to match responses from the controller. When the response matches it considers a command to be complete. Defining commands tells the driver what to send to your controller when OpenPnP wants to perform a certain action.

## Variable Substitution

All of the commands support variable substitution. Variables are in the form of {VariableName:Format}. The variable names available to each command are listed with the command below. The format is a [Java style format string](https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html), similar to printf. If no format is specified the format defaults to `%s`, which simply converts the variable's value to a string.

In the commands below, if a command has variables available they are listed in a table after the command.

## Commands

See [[GcodeDriver: Command Reference]] for the full list of commands, variables and examples.

## Regular Expressions (Receiving Responses)

### COMMAND_CONFIRM_REGEX

The driver uses this regex to look for responses from the controller. After sending a command it will wait for a line that matches this regex before considering the command complete. For many controllers this is simply `ok`, although since some controllers send additional information with command results it's better to use `^ok.*`.

If your controller does not return an OK following the value returned from the ACTUATOR_READ_COMMAND, then it will be necessary to include this response here as well. For example:  "^(ok)|(Read:).*".

### COMMAND_ERROR_REGEX

The driver uses this regex to check for errors in responses from the controller. If the regex is set, and it matches one of the responses an error will be thrown and the response message included. If your controller is able to send errors for invalid or improper commands, you can use this regex to make sure OpenPnP will stop when an error is received.

Example: `^error:.*`

### MOVE_TO_COMPLETE_REGEX

If specified, the driver will check for this regex in the responses after a move-to-command is sent and will not return until the regex is matched. This can be used to support motion controllers that return the command confirmation before movement is complete.

Example: `.*vel:0.00.*`

### ACTUATOR_READ_REGEX

Used to parse a value from an actuator after sending a ACTUATOR_READ_COMMAND. The regex should contain a named group called Value that includes the required response value.

Example: `read:(?<Value>-?\d+)`

This would read a response from the controller in the form of `read:255`. The regex is broken down like this:

1. `read:` is fixed text that the controller sends before the value.
2. The parentheses around the rest of the regex mark everything else as the value we want to capture. This is called a capturing group.
3. The `?<Value>` gives the capturing group the name "Value", which OpenPnP will use to read the result.
4. `-?` allows for an optional negative sign before the value.
5. `\d+` means one or more digits, which represent the value itself.

### POSITION_REPORT_REGEX

Used to parse a position report. Position reports can be sent by the controller to update OpenPnP when a move has been made outside of the program. This is particularly useful for controllers that support external jogging such as manual jog pendants.

The regex should contain a named group for each axis that it wishes to update.

Example: `<Idle,MPos:(?<x>-?\d+\.\d+),(?<y>-?\d+\.\d+),(?<z>-?\d+\.\d+),(?<rotation>-?\d+\.\d+)>`

## Miscellaneous

### units

The units of measure that is used by the controller. Millimeters is most common, although Meters, Centimeters, Microns, Feet, Inches, and Mils are supported as well. This is used internally to convert location data before sending moveTo commands.

### max-feed-rate

The maximum feed rate value that will ever be sent in a move-to-command. The actual value sent will be less than or equal to this value.

### connect-wait-time-milliseconds

Number of milliseconds to wait after connecting to the serial port before sending any commands. This is useful if you have a controller that resets on connect or takes a few seconds to start responding.

## Sub-Drivers

Sub-Drivers allow you to interface multiple controllers by passing commands through. This can be used to control additional boards attached to your machine for functions such as feeders, actuators, conveyers, lighting, etc.

When a command is sent to the driver it is first processed by the main driver and then it is passed on to each defined sub-driver. If the main driver or any sub-driver does not define a handler for the command it is simply ignored.

For example, let's say you have your machine setup to use a Smoothieboard and you are using GcodeDriver to control it. You will have configured various commands such as the `move-to-command`, `pick-command`, `place-command`, etc.

Now perhaps you'd like to add an automatic feeder but don't have any I/O left on your Smoothieboard. You could add an Arduino connected by USB serial and control it with a sub-driver. On the sub-driver you would **only** define `actuate-boolean-command`.

Now when OpenPnP sends a movement command it will be processed by the main driver, which will move the machine. It will then be passed to the sub-driver, but since the sub-driver doesn't have a `move-to-command` it will be ignored. Then when OpenPnP wants to actuate your feeder it will send an actuate command. The main driver does not have `actuate-boolean-command` defined so it ignores it and passes it on to the sub-driver which processes it.

Using this system you can build up a series of controllers of any complexity that you like and control as many devices as you need.

Here is an example sub-drivers section of the main driver configuration:

```
<sub-drivers class="java.util.ArrayList">
   <reference-driver class="org.openpnp.machine.reference.driver.GcodeDriver" port-name="/dev/tty.usbmodem1A12421" baud="9600" flow-control="Off" data-bits="Eight" stop-bits="One" parity="None" set-dtr="false" set-rts="false" units="Millimeters" max-feed-rate="50000" timeout-milliseconds="5000" connect-wait-time-milliseconds="750">
      <command type="COMMAND_CONFIRM_REGEX">
         <text><![CDATA[^ok.*]]></text>
      </command>
      <command type="ACTUATE_DOUBLE_COMMAND">
         <text><![CDATA[{Index}]]></text>
      </command>
   </reference-driver>
</sub-drivers>
```

## Axis Mapping

If your system has more than one nozzle you will need to tell OpenPnP which axes on your controller map to which nozzles, and other devices. Axis Mapping allows you to do this, along with specifying axes that should be ignored or included for a given head mounted device. This is an advanced option and will not be used by everyone. By default OpenPnP will create a basic axis mapping configuration that will work for a single nozzle, four axis system.

See [[GcodeDriver: Axis Mapping]] for the full documentation of this feature.

# Visual Homing

GcodeDriver supports the ability to complete the homing operation using a vision check. If there is a part defined in your Parts list called `FIDUCIAL-HOME` then after GcodeDriver completes the standard homing operation (using `HOME_COMMAND`) it will do a fiducial check for the `FIDUCIAL-HOME` part and reset the home coordinates.

To use visual homing:

1. Create a Part called `FIDUCIAL-HOME`. The Part should follow the same rules are used for setting up [[Fiducials]].
2. Set the GcodeDriver POST_VISION_HOME_COMMAND as [as described in the Reference](https://github.com/openpnp/openpnp/wiki/GcodeDriver:-Command-Reference#post_vision_home_command). This command is sent after visual homing is complete and will reset the coordinates to the home coordinates.
3. Set up your machine so that when mechanical homing is complete the fiducial is visible to the camera.
4. When mechanical homing is complete, GcodeDriver will look for the fiducial, center on it and then reset the X and Y coordinates to the home coordinates.

If your homing fiducial is in a different location than the camera can see after homing you can change the location that is searched by adding `<homing-fiducial-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>` to your driver in `machine.xml`.

# Backlash Compensation

The GcodeDriver includes basic basic backlash compensation; also known as slack compensation. When enabled, the machine will always overshoot the target position and then move back to the target at a slower rate. This has been found to resolve issues with improperly tensioned belts and other sources of backlash.

To enable backlash compensation, there are two steps:

1. In the GcodeDriver configuration, set the backlash offset X and Y to a small value that you think is greater than your backlash. Starting with -0.4mm is a good choice. Also set the backlash feed rate factor to 0.1. The max feed rate is multiplied by this value for the backlash move, so using 0.1 will move the head at 10% of it's normal speed for the final position approach.
2. In the GcodeDriver Gcode configuration, change your move to command to look something like:
```
G0 {BacklashOffsetX:X%.4f} {BacklashOffsetY:Y%.4f} {Z:Z%.4f} {Rotation:A%.4f} F{FeedRate:%.0f}
G1 {X:X%.4f} {Y:Y%.4f} {Z:Z%.4f} {Rotation:A%.4f} F{BacklashFeedRate:%.0f}
```

The way this works is that the first command moves past the target position by the backlash offset amount at the normal feed rate. The second command then moves to the final position at the backlash feedrate, which will be slower.

More information about this feature can be found in https://github.com/openpnp/openpnp/issues/318.

# Non-Squareness Compensation

No matter how well you planned your pnp machine and how well you assembled it chances are that the X and Y moving axes are not 100% perpendicular to each other. A very small 0.1° alignment error will already result in a physical offset of 0.52mm (already a bit more than the width of one 0402 component) when the head travels 300mm. 

The GCodeDriver can compensate such non squareness with a "Non-Squareness Factor". This factor basically tells the ratio between X offset and Y movement distance.

![](https://user-images.githubusercontent.com/9963310/42752246-92e0d1a0-88ed-11e8-81a1-c46651d89eac.png)

**Measurement method:**

1. Make sure the current Non-Squareness Factor in the GcodeDriver settings panel is zero.
2. Put a piece of graph paper with fine grid into your PnP machine. 
3. Align the grid with the X-Axis by moving the camera left and right until a reference grid line matches the motion path exactly.
4. Move the camera into Y direction along a grid line by a defined distance (the further you move the more precise the measurement will be). 100mm is a good start.
5. You will probably find that the camera has not traced the Y line exactly and now it is offset from the center of the camera. Click the DRO and use the relative coordinates to measure the offset from the line.
6. Use the formula in the image above to calculate the Non-Squareness Factor. Positive factors mean a machine leaning left, negatives ones leaning right. 
7. Enter the Non-Squareness Factor into the GcodeDriver settings panel and apply. 
8. Repeat steps 3 to 5. This time there should be no offset. If the offset is even greater try inverting the sign of the Non-Squareness Factor. 


# Troubleshooting

## Nozzle is Moving or Turning When Camera is Moved

If your nozzle is moving when the camera is commanded to move, especially in Z, you need to set up [[GcodeDriver: Axis Mapping]] so that the camera is not included in the Z axis. This is done by specifying only the Nozzle IDs in the Z axis instead of "*". For more information see [[GcodeDriver: Axis Mapping]].
This solve also a limited speed of moves in the x,y,z axis, if the camera is commanded to turn and the nozzle rotates as well (rather slowly). 