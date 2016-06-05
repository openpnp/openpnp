# Pre-Release Warning: This feature is not yet finished. The documentation is not complete. See https://github.com/openpnp/openpnp/issues/106

GcodeDriver is a universal driver that makes it possible to configure complex machines and add-on hardware such as feeders without having to write any custom driver code. All configuration is done through commands in the configuration files.

GcodeDriver introduces a new concept to OpenPnP drivers: Sub-Drivers. Sub-Drivers allow you to embed multiple drivers in your configuration and each one can control a different device.

In the video below there are two GcodeDrivers running. One is controlling the Smoothieboard running the machine and one is controlling an Arduino running a simple sketch. This is intended to demonstrate how you can easily integrate additional hardware such as feeders.

Demonstration Video (Pay attention to the red LEDs): https://www.youtube.com/watch?v=0ntYOy0s_8Y

# Configuration
## Variable Substitution

All of the commands below support variable substitution. Variables are in the form of {VariableName:Format}. The variable names available to each command are listed with the command below. The format is a [Java style format string](https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html), similar to printf. If no format is specified the format defaults to `%s`, which simply converts the variable's value to a string.

In the commands below, if a command has variables available they are listed in a table after the command.

## Command List

This is a list of Gcode configuration options available for the GcodeDriver. Each of these options can be specified in the `machine.xml` or in the (TODO: configuration panel). When the driver is commanded by OpenPnP to perform an action it looks up the appropriate Gcode, performs variable substitution and then sends it to the controller.

Commands may contain newline characters. Each line is sent to the controller in turn, waiting for a response before sending the next.

### connect-command

Sent after the driver finishes connecting to the serial port. Can be used to send any initialization parameters the controller needs.

Example:
```
G21 ; Set millimeters mode
G90 ; Set absolute positioning mode
M82 ; Set absolute mode for extruder
```

### enable-command

Sent when the machine is enabled, primarily when the big START button is pressed. Can be used to turn on motors and lighting, start pumps, reset solenoids, etc.

Example:
```
M810 ; Turn on LED lighting
```

### disable-command

Sent when the machine is disabled, primarily when the big STOP button is pressed or before shutting down. Should turn off everything.

Example:
```
M84 ; Disable steppers
M811 ; Turn off LED lighting
```

### home-command

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

### move-to-command

This command has special handling for the X, Y, Z and Rotation variables. If the move does not change one of these variables that variable is replaced with the empty string, removing it from the command. This allows Gcode to be sent containing only the components that are being used which is important for some controllers when moving an "extruder" for the C axis. The end result is that if a move contains only a change in the C axis only the C axis value will be sent.

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| Id             | String   | User defined id of the HeadMountable (Nozzle, Camera, Actuator) being homed. |
| Name           | String   | User defined name of the HeadMountable (Nozzle, Camera, Actuator) being homed. |
| X              | Double   | The calculated X position for the move. |
| Y              | Double   | The calculated Y position for the move. |
| Z              | Double   | The calculated Z position for the move. |
| Rotation       | Double   | The calculated C or Rotation position for the move. |
| FeedRate       | Double   | The calculated feed rate for the move. |

Example:
```
G0 {X:X%.4f} {Y:Y%.4f} {Z:Z%.4f} {Rotation:E%.4f} F{FeedRate:%.0f} ; Send standard Gcode move
M400 ; Wait for moves to complete before returning
```

### pick-command

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

### place-command

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

### actuate-boolean-command

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

### actuate-double-command

Sent whenever an Actuator's actuate(double) method is called. This is currently used by the ReferenceAutoFeeder to trigger a feed operation. Actuators are generally an area where people customize their machines, so this is here to support customizations such as automated feeders.

| Variable Name  |   Type   | Description |
| -------------- | -------- | ----------- |
| Id             | String   | The user defined id of the actuator. |
| Name           | String   | The user defined name of the actuator. |
| Index          | Index    | The user defined index of the actuator. Can be used to specify a register or port number. |
| DoubleValue    | Double   | The Double value sent to the actuator. This is typically user defined in the configuration of the device using the actuator. |
| IntegerValue   | Integer | The Double value sent to the actuator after being cast to an Integer. This is typically user defined in the configuration of the device using the actuator. |

## Regular Expressions (Receiving Responses)

### command-confirm-regex

The driver uses this regex to look for responses from the controller. After sending a command it will wait for a line that matches this regex before considering the command complete. For many controllers this is simply `ok`, although since some controllers send additional information with command results it's better to use `^ok.*`.

### move-to-complete-regex

If specified, the driver will check for this regex in the responses after a move-to-command is sent and will not return until the regex is matched. This can be used to support motion controllers that return the command confirmation before movement is complete.

Example: `<move-to-complete-regex>.*vel:0.00.*</move-to-complete-regex>`

## Miscellaneous

### units

The units of measure that is used by the controller. Millimeters is most common, although Inches is supported as well. This is primarily used internally to convert location data before sending moveTo commands.

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
   <reference-driver class="org.openpnp.machine.reference.driver.GcodeDriver" port-name="/dev/tty.usbmodem1A12421" baud="9600" units="Millimeters" max-feed-rate="50000" connect-wait-time-milliseconds="750">
      <home-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
      <command-confirm-regex>^ok.*</command-confirm-regex>
      <actuate-double-command>{Index}</actuate-double-command>
      <sub-drivers class="java.util.ArrayList"/>
   </reference-driver>
</sub-drivers>
```

## Axis Mapping

If your system has more than one nozzle you will need to tell OpenPnP which axes on your controller map to which nozzles, and other devices. Axis Mapping allows you to do this, along with specifying axes that should be ignored or included for a given head mounted device. This is an advanced option and will not be used by everyone. By default OpenPnP will create a basic axis mapping configuration that will work for a basic system.

### Defining Axes

The first step in axis mapping is to define each axis that your motion controller can control. In general these will correspond to motors on your machine, but not always. Another way to think of it is to consider each individual part of the machine that can move.

For instance, a common machine might have an X axis, a Y axis, a Z motor that controls two Z axes for two nozzles and a C or Rotation motor for each Nozzle. This machine has 5 axes: X, Y, Z, C1, C2.

A basic axis definition looks like:
```
   <axis name="x" type="X" home-coordinate="0.0">
      <head-mountable-ids class="java.util.HashSet">
         <string>*</string>
      </head-mountable-ids>
   </axis>
```

### Mapping Axes to HeadMountables

In OpenPnP anything that can be attached to the head is called a HeadMountable. These are your Nozzles, PasteDispensers, Cameras and Actuators. Each of these has an ID.

In the example we defined above, the machine has two nozzles that we'll call N1 and N2. The two nozzles share a Z axis and each has it's own C axis.

**TODO: Finish this doc.**

### Complete Sample Axis Mapping

```         
 <axes class="java.util.ArrayList">
   <axis name="x" type="X" home-coordinate="0.0">
      <head-mountable-ids class="java.util.HashSet">
         <string>*</string>
      </head-mountable-ids>
   </axis>
   <axis name="y" type="Y" home-coordinate="0.0">
      <head-mountable-ids class="java.util.HashSet">
         <string>*</string>
      </head-mountable-ids>
   </axis>
   <axis name="z" type="Z" home-coordinate="0.0">
      <head-mountable-ids class="java.util.HashSet">
         <string>69edd567-2341234-495a-9b30-2fcbf5c9742f</string>
         <string>69edd567-df6c-495a-9b30-2fcbf5c9742f</string>
      </head-mountable-ids>
      <transform class="org.openpnp.machine.reference.driver.GcodeDriver$NegatingTransform">
         <negated-head-mountable-id>69edd567-2341234-495a-9b30-2fcbf5c9742f</negated-head-mountable-id>
      </transform>
   </axis>
   <axis name="rotation" type="Rotation" home-coordinate="0.0">
      <head-mountable-ids class="java.util.HashSet">
         <string>*</string>
      </head-mountable-ids>
   </axis>
</axes>
```

# Sample Configuration
```
<openpnp-machine>
   <machine class="org.openpnp.machine.reference.ReferenceMachine">
      <heads>
         <head class="org.openpnp.machine.reference.ReferenceHead" id="22964dce-252a-453e-8106-65db104a0763" name="H1">
            <nozzles>
               <nozzle class="org.openpnp.machine.reference.ReferenceNozzle" id="69edd567-df6c-495a-9b30-2fcbf5c9742f" name="N1" pick-dwell-milliseconds="500" place-dwell-milliseconds="500" current-nozzle-tip-id="e092921a-2eef-449b-b340-aa3f40d8d791" changer-enabled="false" limit-rotation="true">
                  <nozzle-tips>
                     <nozzle-tip class="org.openpnp.machine.reference.ReferenceNozzleTip" id="e092921a-2eef-449b-b340-aa3f40d8d791" name="NT1" allow-incompatible-packages="true">
                        <compatible-package-ids class="java.util.HashSet"/>
                        <changer-start-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                        <changer-mid-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                        <changer-end-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                     </nozzle-tip>
                  </nozzle-tips>
                  <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                  <safe-z value="0.0" units="Millimeters"/>
               </nozzle>
            </nozzles>
            <actuators/>
            <cameras>
               <camera class="org.openpnp.machine.reference.camera.ImageCamera" id="d66faf53-05e1-4629-baae-b614c5ed8320" name="Zoom" looking="Down" settle-time-ms="250" rotation="0.0" flip-x="false" flip-y="false" offset-x="0" offset-y="0" fps="24" width="640" height="480">
                  <units-per-pixel units="Millimeters" x="0.04233" y="0.04233" z="0.0" rotation="0.0"/>
                  <vision-provider class="org.openpnp.machine.reference.vision.OpenCvVisionProvider"/>
                  <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                  <safe-z value="0.0" units="Millimeters"/>
                  <calibration enabled="false">
                     <camera-matrix length="9">6.08418777596376E-310, 7.63918484747E-313, 6.9520291754075E-310, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0</camera-matrix>
                     <distortion-coefficients length="5">6.95202917515477E-310, 0.0, 0.0, 0.0, 0.0</distortion-coefficients>
                  </calibration>
                  <source-uri>classpath://samples/pnp-test/pnp-test.png</source-uri>
               </camera>
            </cameras>
            <paste-dispensers>
               <paste-dispenser class="org.openpnp.machine.reference.ReferencePasteDispenser" id="53050ccf-59a0-4d9f-a8d3-6216f5412e4e" name="D1">
                  <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                  <safe-z value="0.0" units="Millimeters"/>
               </paste-dispenser>
            </paste-dispensers>
         </head>
      </heads>
      <feeders>
         <feeder class="org.openpnp.machine.reference.feeder.ReferenceAutoFeeder" id="ec54bc2c-b18d-4088-aca5-dab09f5bf3d0" name="ReferenceAutoFeeder" enabled="true" part-id="R0201-1K" actuator-name="A1" actuator-value="0.0">
            <location units="Millimeters" x="100.0" y="-200.0" z="-20.0" rotation="0.0"/>
         </feeder>
         <feeder class="org.openpnp.machine.reference.feeder.ReferenceAutoFeeder" id="aed8f089-f65d-42e6-baa6-d65bebd567a6" name="ReferenceAutoFeeder" enabled="true" part-id="R0402-1K" actuator-name="A2" actuator-value="0.0">
            <location units="Millimeters" x="110.0" y="-200.0" z="-20.0" rotation="0.0"/>
         </feeder>
         <feeder class="org.openpnp.machine.reference.feeder.ReferenceAutoFeeder" id="de8de97f-fef7-4c5d-a525-e2e4ce257ff8" name="ReferenceAutoFeeder" enabled="true" part-id="R0603-1K" actuator-name="A3" actuator-value="0.0">
            <location units="Millimeters" x="120.0" y="-200.0" z="-20.0" rotation="0.0"/>
         </feeder>
         <feeder class="org.openpnp.machine.reference.feeder.ReferenceAutoFeeder" id="53a97089-1bf8-4632-b91b-3f7f3d46362c" name="ReferenceAutoFeeder" enabled="true" part-id="R0805-1K" actuator-name="A4" actuator-value="0.0">
            <location units="Millimeters" x="130.0" y="-200.0" z="-20.0" rotation="0.0"/>
         </feeder>
      </feeders>
      <cameras/>
      <actuators>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="A1" name="A1" index="0">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
            <safe-z value="0.0" units="Millimeters"/>
         </actuator>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="A2" name="A2" index="1">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
            <safe-z value="0.0" units="Millimeters"/>
         </actuator>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="A3" name="A3" index="2">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
            <safe-z value="0.0" units="Millimeters"/>
         </actuator>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="A4" name="A4" index="3">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
            <safe-z value="0.0" units="Millimeters"/>
         </actuator>
      </actuators>
      <job-processors class="java.util.HashMap">
         <job-processor type="PickAndPlace">
            <job-processor class="org.openpnp.machine.reference.ReferenceJobProcessor" demo-mode="false">
               <job-planner class="org.openpnp.planner.SimpleJobPlanner"/>
            </job-processor>
         </job-processor>
      </job-processors>
      <driver class="org.openpnp.machine.reference.driver.GcodeDriver" port-name="/dev/tty.usbmodem1A1211" baud="115200" units="Millimeters" max-feed-rate="15000" connect-wait-time-milliseconds="0">
         <home-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
         <command-confirm-regex>^ok.*</command-confirm-regex>
         <connect-command>G21
G90
M82</connect-command>
         <enable-command>M810</enable-command>
         <disable-command>M84
M811</disable-command>
         <home-command>M84
G4P500
G28 X0 Y0
G92 X0 Y0 Z0 E0</home-command>
         <move-to-command>G0 {X:X%.4f} {Y:Y%.4f} {Z:Z%.4f} {Rotation:E%.4f} F{FeedRate:%.0f}
M400</move-to-command>
         <pick-command>M808
M800</pick-command>
         <place-command>M809
M801
M802
G4P250
M803</place-command>
         <sub-drivers class="java.util.ArrayList">
            <reference-driver class="org.openpnp.machine.reference.driver.GcodeDriver" port-name="/dev/tty.usbmodem1A12421" baud="9600" units="Millimeters" max-feed-rate="50000" connect-wait-time-milliseconds="750">
               <home-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
               <command-confirm-regex>^ok.*</command-confirm-regex>
               <actuate-double-command>{Index}</actuate-double-command>
               <sub-drivers class="java.util.ArrayList"/>
            </reference-driver>
         </sub-drivers>
      </driver>
   </machine>
</openpnp-machine>
```

# Sample Arduino Sketch
```
String inData;

void setup() {
  for (int i = 0; i < 4; i++) {
    pinMode(2 + i, OUTPUT);
    digitalWrite(2 + i, LOW);
  }
  
  Serial.begin(9600);
  Serial.println("Feeduino");
  Serial.println("ok");
}

void loop() {
    while (Serial.available() > 0) {
      char recieved = Serial.read();
      inData += recieved; 

      if (recieved == '\n') {
        processCommand();
        inData = "";
      }
  }
}

void processCommand() {
  int pin = inData.toInt();
  digitalWrite(2 + pin, HIGH);
  delay(500);
  digitalWrite(2 + pin, LOW);
  Serial.println("ok");
}
```