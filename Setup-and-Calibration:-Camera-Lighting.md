You can use OpenPnP's [Actuator](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Actuators) system to turn lights on and off in response to camera capture events.

# Set Up a Lighting Actuator

1. Add an actuator for the lights you want to control. Go to the Machine Setup tab and then look for the Camera you want to assign the light to. You'll find it either attached to the Machine (up-looking Camera) or to the Head (down-looking Camera). Got to the Actuators branch besides the Cameras branch and then click the green + button to add a new Actuator.
2. Expand the Actuators list and select the new Actuator.
3. Set up the Actuator as described in the [Actuators page](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Actuators). If you have multi-channel lights, be sure to read about the Actuator Profiles. 

# Test The Lights

Go to Main Window -> Machine Controls -> Actuators and click the button named after the created Actuator. Your lights should come on. Click it again and they should turn off. If this doesn't work, adjust your settings until it does, or ask for help.

# Assign the Camera Light Actuator

The created **Light Actuator** can now be assigned to the camera:

![Camera Light Actuator](https://user-images.githubusercontent.com/9963310/104466458-8db7b300-55b5-11eb-8674-b554fc44ce54.png)

There are various ON and OFF actuation Options:

* **Before Capture ON**: actuate the Light Actuator before computer vision captures a camera image. This happens before the [Camera Settling](/openpnp/openpnp/wiki/Camera-Settling). See the [ImageCapture Stage](#use-in-the-imagecapture-stage) section to control pipeline specific light actuation values and profiles. **NOTE:** This option should almost always be enabled. 
* **After Capture OFF**: actuate the Light Actuator OFF after computer vision captured a camera image. 
* **User Camera Action ON**: actuate the Light Actuator ON when a user action is deliberately positioning or otherwise using the camera. 
* **Anti-Glare OFF**: prevent this camera light from blinding another camera, it is actuated OFF before any other camera is capturing an image. Only cameras looking the other way (up/down) are taken into consideration (see the **Looking** field). 

As soon as a **Light Actuator** is assigned, the Camera View will display a new overlay light symbol in the upper right corner. It shows the camera light status and can be clicked to turn the light on or off:
 
![CameraViewLightOnOff](https://user-images.githubusercontent.com/9963310/103424966-9e2c5e80-4baf-11eb-8eac-a20844fed4e8.gif)

# Use in the ImageCapture Stage 

Every computer vision pipeline typically contains the ImageCapture stage to capture an image from the camera:

![ImageCapture Stage](https://user-images.githubusercontent.com/9963310/103425063-2874c280-4bb0-11eb-9ce2-e2ce715f39a9.png)

As soon as the camera has a **Light Actuator** assigned, the ImageCapture stage can control the lighting. 

If the **defaultLight** property is switched on (default) the **Default ON** value of the actuator will be used to determine the lighting. If it is switched off, the **light** property comes into play.

The **light** property controls the lighting for this specific pipeline (if **defaultLight** is off). The user interface responds to the [**Value Type** of the actuator](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Actuators#actuator-value-type), i.e. it can be a checkbox for lights that can only switch ON/OFF, it can be a number for lights that can be controlled in intensity, or it can be an [Actuator with Profiles](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Actuators#actuator-with-profiles) combo box to control multi-channel lights:

![Light Property Boolean](https://user-images.githubusercontent.com/9963310/103425237-53abe180-4bb1-11eb-93f0-b212953113f5.png) 

![Light Property Double](https://user-images.githubusercontent.com/9963310/103425242-61f9fd80-4bb1-11eb-949d-0c62382d5c65.png)  

![Light Property Profile](https://user-images.githubusercontent.com/9963310/103425285-a7b6c600-4bb1-11eb-9ba6-b94eaa55a34c.png)

# Add Lighting Control Scripts
As an alternative to controlling the actuators directly from OpenPnP, you can use scripts to do so. This was the only option in earlier versions of OpenPnP, so this documentation is mostly kept in order to support the older versions. 
___
Not recommended for new machines! 
___

**The following is for (legacy) OpenPnP 2.0 version only, for versions 1.0 see further below.**

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
| [Vacuum Sensing](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Vacuum-Sensing) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [[Computer Vision]] |