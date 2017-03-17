# GcodeDriver Configuration (Under Construction)

GcodeDriver is a universal driver for any controller that uses Gcode. It can even be used with controllers that don't use Gcode, as long as they accept basic text based commands. You should use GcodeDriver unless you have a very unique machine that requires a special, custom driver.

This page is intended to guide the reader through a basic GcodeDriver setup. For more advanced information, see [[the complete GcodeDriver reference|GcodeDriver]].

## Preparation

Before starting to configure the driver you should collect some information about your machine:
1. What kind of controller are you using? Some common ones are SmoothieBoard, Cohesion3D ReMix (Smoothie), Grbl, Marlin and TinyG.
2. Find the command reference for your controller:
    * [Smoothie](http://smoothieware.org/supported-g-codes)
    * [Grbl](https://github.com/gnea/grbl/blob/master/doc/markdown/commands.md)
    * [Marlin](http://marlinfw.org/meta/gcode/)
    * [TinyG](https://github.com/synthetos/TinyG/wiki/Gcode-Support)
3. Make sure you've selected the GcodeDriver in OpenPnP and restarted it. If you haven't done that yet, see [Setup and Calibration: Driver Setup](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Driver-Setup).

## Document The Hardware

Next you'll document all the hardware in the machine. This will help you determine what commands you need to configure the driver with.

1. Make a list of each device on the machine you want to control from OpenPnP. This should include motors, pumps, solenoid valves, feeders, cameras, camera lighting, etc.
    
    As an example, my machine hardware list looks like this: X stepper, Y stepper, Z stepper, nozzle 1 rotation stepper, nozzle 2 rotation stepper, top camera, bottom camera, camera LEDs, vacuum pump, nozzle 1 vacuum solenoid, nozzle 1 exhaust solenoid, nozzle 2 vacuum solenoid, nozzle 2 exhaust solenoid.
2. Now go back to the list and write down the commands needed to control each device. These will typically be Gcode G or M commands.

    Use the reference you found above, along with a tool like [Printrun](https://github.com/kliment/Printrun) to make sure you can control every device.

    If your controller requires commands for startup, homing, enable, disable, etc. you should add those items to the list too.

    Your list should now look something like this:
    * Connect: G21, G90, M82
    * Enable: M801, M803, M805, M807, M809, M810
    * Disable: M801, M803, M805, M807, M809, M811
    * Home: M84, G92 Z0, G28 X0 Y0, T1, G92 E0, T0 G92 E0
    * X stepper: G0 Xnnn Fnnn
    * Y stepper G0 Ynnn Fnnn
    * Z stepper G0 Znnn Fnnn
    * nozzle 1 rotation stepper: T0 G0 Ennn Fnnn
    * nozzle 2 rotation stepper: T1 G0 Ennn Fnnn
    * top camera: USB
    * bottom camera: USB
    * camera LEDs: M810 on, M811 off
    * vacuum pump: M808 on, M809 off
    * nozzle 1 vacuum solenoid: M800 on, M801 off
    * nozzle 1 exhaust solenoid: M802 on, M803 off
    * nozzle 2 vacuum solenoid: M804 on, M805 off
    * nozzle 2 exhaust solenoid M806 on, M807 off
3. Make note of how many axes your machine has, and how many motors you have attached to each axis. For instance, the most basic machine has four axes: X, Y, Z, Rotation, and each one has it's own stepper motor. If you have multiple nozzles you will probably have additional Rotation axes and motors and depending on how your Z axis works you may have more than one Z axis controlled by one Z motor.

## Multiple Controllers

The GcodeDriver supports controlling multiple controllers at once. This uses a concept called sub-drivers that will be explained below. For now, just remember that if you have multiple controllers you'll need to know which one controls each device too.

## Axis Mapping (TODO)

## Configure Primary Controller

Your primary controller is typically the one that controls X and Y. If you don't have multiple controllers then this is the only one you'll need to configure.

### Serial Port
1. Open the GcodeDriver settings by going to Machine Setup -> Driver -> GcodeDriver and selecting it. The GcodeDriver configuration panel will open below.
2. In the settings below, open the Serial tab.
3. Select the port for your controller from the dropdown.
4. Select the baud rate.
5. Select any other less common settings that your serial port requires.
6. Click Apply.

### General Settings
1. Select the General Settings tab.
2. Set the Units that your controller uses for movement.
3. Set the max feed rate in Units per Minute. This should match the maximum setting in your controller.
4. If your controller requires a long time to respond after being opened, increase the Connect Wait Timeout.
5. If you have commands that take a very long time to run, increase the Command Timeout.
6. Click Apply before moving on.

### Gcode
The Gcode tab is where you will add all the commands you listed above. You will also set some special commands that tell OpenPnP how to talk to your controller. If you have more than one controller, only add the commands below for the primary controller. We'll do the secondary controllers later.

1. In the Head Mountable dropdown, select Default.
2. In the Setting dropdown, select COMMAND_CONFIRM_REGEX. The setting will be shown in the text field below.
3. Take note of the default setting, which is `^ok.*`. This is a pattern that OpenPnP uses to match against responses from your controller. Almost every controller just responds with "ok", so just use the default unless your know that your controller sends a different kind of response to a command.
4. Select CONNECT_COMMAND from the Setting dropdown. You'll see the default below and you should replace this with the Connect command, if any, that you recorded above. The CONNECT_COMMAND is sent when OpenPnP connects to the controller.
5. Select ENABLE_COMMAND and do the same as above. The ENABLE_COMMAND is sent whenever you click the green power button in OpenPnP.
6. Choose DISABLE_COMMAND and do the same. DISABLE_COMMAND is sent when you click the red power button.
7. Do the same for HOME_COMMAND, PUMP_ON_COMMAND, PUMP_OFF_COMMAND. If you only have one nozzle, you can also configure PICK_COMMAND and PLACE_COMMAND. PICK_COMMAND is sent when OpenPnP wants to pick up a part. It usually turns on a solenoid valve. PLACE_COMMAND is used when OpenPnP is ready to place the part. It usually turns the same solenoid back off.
8. Choose MOVE_TO_COMMAND. MOVE_TO_COMMAND is sent any time OpenPnP needs to move the head of the machine. The command includes the position that the controller should move to for each of the four active axes: X, Y, Z, C (Rotation).

    The default command will work for Smoothie based controllers:
    ```
    G0 {X:X%.4f} {Y:Y%.4f} {Z:Z%.4f} {Rotation:E%.4f} F{FeedRate:%.0f} ; Send standard Gcode move
    M400 ; Wait for moves to complete before returning
    ```

    The default command sends a standard Gcode movement command and uses variables to substitute in the position that OpenPnP wants to move to. It also include the FeedRate variable so that you can control the speed of movements from within OpenPnP.

    After the movement command, the default sends a `M400` command. This causes Smoothie not to respond `ok` until all motion is complete. It's very important that your command include something like this so that OpenPnP doesn't try to move again before motion has stopped.

    If needed, edit the commands to match those that you recorded for your controller. Note that some controllers, like TinyG, don't have a command that will cause it to wait. Instead you have to tell OpenPnP to look for a special response. If your controller is like this, you'll also need to configure MOVE_TO_COMPLETE_REGEX. TinyG, for example, should use `.*stat:3.*`.
9. Click Apply.

## Test Primary Controller
You should now be able to connect to the controller and start trying out some of the devices you configured.

1. Click the green power button and connect to your controller. OpenPnP will open the serial port, wait a second or two and then send the CONNECT_COMMAND. Once it connects it will send the ENABLE_COMMAND. You can see what is happening in the Log tab.
2. Click the Home button and the machine should perform it's homing operation.
3. Adjust the jog Distance slider to 1.0mm and try jogging in every direction by clicking the jog arrow buttons. See if the machine moves in the directions you expect it to.
4. Go to the Feeders tab and add a ReferenceTubeFeeder. Click the Feed and Pick button to test your PICK_COMMAND. OpenPnP should turn on the vacuum pump and the vacuum solenoid.
5. Go to the Machine Controls section and find the Special tab. Click Discard to test the PLACE_COMMAND. OpenPnP will turn off the vacuum pump and the vacuum solenoid.


