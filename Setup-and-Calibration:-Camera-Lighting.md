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

# Use in the ImageCapture Stage 

Every computer vision pipeline typically contains the ImageCapture stage to capture an image from the camera:

![ImageCapture Stage](https://user-images.githubusercontent.com/9963310/103425063-2874c280-4bb0-11eb-9ce2-e2ce715f39a9.png)

As soon as the camera has a **Light Actuator** assigned, the ImageCapture stage can control the lighting. 

If the **defaultLight** property is switched on (default) the **Default ON** value of the actuator will be used to determine the lighting. If it is switched off, the **light** property comes into play.

The **light** property controls the lighting for this specific pipeline (if **defaultLight** is off). The user interface responds to the value type of the actuator:

![Light Property Boolean](https://user-images.githubusercontent.com/9963310/103425237-53abe180-4bb1-11eb-93f0-b212953113f5.png) ![Light Property Double](https://user-images.githubusercontent.com/9963310/103425242-61f9fd80-4bb1-11eb-949d-0c62382d5c65.png)  

![Light Property Profile](https://user-images.githubusercontent.com/9963310/103425285-a7b6c600-4bb1-11eb-9ba6-b94eaa55a34c.png)

# Add Lighting Control Scripts

**The following is for OpenPnP 2.0 only**:

For newer versions of OpenPnP 2.0, you must use the Camera.BeforeSettle/Camera.AfterSettle scripts, instead of the Capture ones. See also [[Scripting#CameraBeforeSettle]], [[Scripting#CameraAfterSettle]] and the page about [[Camera Settling]].

1. Download the [zipfile containing the scripts](https://github.com/ozzysv/Camera-Lighting-openpnp/raw/master/Camera%20Lighting.zip)
2. Expand the zipfile. There should be two scripts inside.
3. Go to Main Menu -> Scripts -> Open Scripts Directory.
4. In the folder that opens, open the Events directory.
5. Copy the two scripts into the Events directory.

Now, whenever OpenPnP needs to capture an image from your Up looking camera it will run the Camera.BeforeSettle.js script, settle the camera and capture an image and then run the Camera.AfterSettle.js script. The first one will turn the lights on and the second will turn the lights off.

You can customize the way this works by modifying the scripts or the Gcode that controls the lights.

If you want to control the Down camera lights, just add another Actuator called DownCamLights. The script is already set up to use it.

**The following is for OpenPnP 1.0 only**:

1. Download the [zipfile containing the scripts](https://gist.github.com/vonnieda/1bed59fe30c637b88470e0ca3cb5d05d/archive/fb9682a01708e3555f44d26469df7c81007be34a.zip) 
to your desktop or somewhere else you can easily find it.
2. Expand the zipfile. There should be two scripts inside.
3. Go to Main Menu -> Scripts -> Open Scripts Directory.
4. In the folder that opens, open the Events directory.
5. Copy the two scripts into the Events directory.

Now, whenever OpenPnP needs to capture an image from your Up looking camera it will run the Camera.BeforeCapture.js script, capture an image and then run the Camera.AfterCapture.js script. The first one will turn the lights on and the second will turn the lights off.

You can customize the way this works by modifying the scripts or the Gcode that controls the lights.

If you want to control the Down camera lights, just add another Actuator called DownCamLights. The script is already set up to use it.

Here is a short video showing the steps above: https://www.youtube.com/watch?v=Y4DbYY9a9BQ


| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [Vacuum Sensing](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Vacuum-Sensing) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Next Steps](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Next-Steps) |