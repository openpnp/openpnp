GcodeDriver is a universal driver that makes it possible to configure complex machines and add-on hardware such as feeders without having to write any custom driver code. All configuration is done through commands in the configuration files.

GcodeDriver introduces a new concept to OpenPnP drivers: Sub-Drivers. Sub-Drivers allow you to embed multiple drivers in your configuration and each one can control a different device. This is helpful for cases where you might have one board that controls your machine's movements and another that controls your feeders.

Work on this feature is being done in #106: https://github.com/openpnp/openpnp/issues/106

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

### COMMAND_ERROR_REGEX

The driver uses this regex to check for errors in responses from the controller. If the regex is set, and it matches one of the responses an error will be thrown and the response message included. If your controller is able to send errors for invalid or improper commands, you can use this regex to make sure OpenPnP will stop when an error is received.

### MOVE_TO_COMPLETE_REGEX

If specified, the driver will check for this regex in the responses after a move-to-command is sent and will not return until the regex is matched. This can be used to support motion controllers that return the command confirmation before movement is complete.

Example: `<move-to-complete-regex>.*vel:0.00.*</move-to-complete-regex>`

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

If your system has more than one nozzle you will need to tell OpenPnP which axes on your controller map to which nozzles, and other devices. Axis Mapping allows you to do this, along with specifying axes that should be ignored or included for a given head mounted device. This is an advanced option and will not be used by everyone. By default OpenPnP will create a basic axis mapping configuration that will work for a basic system.

See [[GcodeDriver: Axis Mapping]] for the full documentation of this feature.

# Troubleshooting

## Asking for Help

If you are asking for help, especially on the mailing list, please follow the instructions below to make sure that you provide enough information for people to help figure out what the problem is.

1. Turn on trace logging. See https://github.com/openpnp/openpnp/wiki/FAQ#how-do-i-turn-on-debug-logging for more information.
2. Perform whatever actions are causing a problem.
3. Shut down OpenPnP.
4. Find the OpenPnP.log file: https://github.com/openpnp/openpnp/wiki/FAQ#where-are-configuration-and-log-files-located
5. Find your machine.xml configuration file: https://github.com/openpnp/openpnp/wiki/FAQ#where-are-configuration-and-log-files-located
6. Send both the machine.xml file and the OpenPnP.log file to whoever you are asking for help.

## Nozzle is Moving When Camera is Moved

If your nozzle is moving when the camera is commanded to move, especially in Z, you need to set up [[GcodeDriver: Axis Mapping]] so that the camera is not included in the Z axis. This is done by specifying only the Nozzle IDs in the Z axis instead of "*". For more information see [[GcodeDriver: Axis Mapping]].