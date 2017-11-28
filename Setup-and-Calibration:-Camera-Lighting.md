You can use OpenPnP's [Actuator](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Actuators) system along with [Scripting Events](https://github.com/openpnp/openpnp/wiki/Scripting#scripting-events) to turn lights on and off in response to camera capture events.

# Set Up a Lighting Actuator
1. Add an actuator for the lights you want to control. Select Machine Setup -> ReferenceMachine -> Actuators and then click the green + button to add a new Actuator.
2. Expand the Actuators list and select the new Actuator.
3. Change the Actuator's name to "UpCamLights" and press Apply.
4. Go to Machine Setup -> ReferenceMachine -> Driver -> GcodeDriver -> Gcode.
5. In the Head Mountable dropdown, select UpCamLights.
6. In the Setting dropdown select ACTUATE_BOOLEAN_COMMAND.
7. In the text area, set the Gcode to the commands that will turn your lights on and off. For example, to send `M800` for on and `M801` for off, use: `{True:M800}{False:M801}`. You should change the two commands to whatever works for your machine.
8. Press the Apply button.
9. Restart OpenPnP.

# Test The Lights
1. Go to Main Window -> Machine Controls -> Actuators and click the UpCamLights button. Your lights should come on. Click it again and they should turn off. If this doesn't work, adjust your Gcode settings until it does, or ask for help.

# Add Lighting Control Scripts
1. Download the [zipfile containing the scripts](https://gist.github.com/vonnieda/1bed59fe30c637b88470e0ca3cb5d05d/archive/fb9682a01708e3555f44d26469df7c81007be34a.zip) to your desktop or somewhere else you can easily find it.
2. Expand the zipfile. There should be two scripts inside.
3. Go to Main Menu -> Scripts -> Open Scripts Directory.
4. In the folder that opens, open the Events directory.
5. Copy the two scripts into the Events directory.

Now, whenever OpenPnP needs to capture an image from your Up looking camera it will run the Camera.BeforeCapture.js script, capture an image and then run the Camera.AfterCapture.js script. The first one will turn the lights on and the second will turn the lights off.

You can customize the way this works by modifying the scripts or the Gcode that controls the lights.

If you want to control the Down camera lights, just add another Actuator called DownCamLights. The script is already set up to use it.

Here is a short video showing the steps above: https://www.youtube.com/watch?v=Y4DbYY9a9BQ

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [Vacuum Sensing](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Vacuum-Sensing) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Next Steps](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Next-Steps) |