# Bottom Camera Setup
The Bottom Camera is a upward looking camera fixed to the machine, and not the head. The Bottom Camera is used during Bottom Vision processing to more accurately place parts. It's not required to have a Bottom Camera, but you will generally get better results if you do have one.

## Add a Camera
First, add a camera by following the instructions in [[General Camera Setup|Setup and Calibration: General Camera Setup]]. Make sure to configure each section before returning to this page to continue.

Many Bottom Cameras tend to use much shorter fisheye lenses, so pay careful attention to the Lens Calibration section in the document above. It's likely to be more important for the Bottom Camera than the Top Camera.

## Set Camera Location
OpenPnP needs to know where on the machine the bottom camera is.

![screen shot 2016-06-18 at 11 38 48 am](https://cloud.githubusercontent.com/assets/1182323/16172994/3fd9c286-3549-11e6-9939-1ee0057c0911.png)

1. Open the Cameras tab and select the Bottom Camera.
2. Select the Camera Specific configuration tab on the right.
3. Scroll down to the Location group.
4. Jog the primary nozzle so that it is perfectly centered and in focus over the camera.
5. Click the capture tool location button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/capture-nozzle.svg) to grab the location of the nozzle, thereby setting the camera location.
6. Press Apply to save your changes.

## Move Nozzle to Camera
Now that OpenPnP knows where the camera is, let's move the nozzle around and make sure everything looks right.

1. Hold Shift and then Left Click the mouse in the camera view for the bottom camera.
2. The nozzle will move to the camera and should land where you clicked.
3. Using the jog controls, move the nozzle forward, backward, left and right while looking at the camera view.
4. Make sure that when you move the nozzle in each direction it moves the same way in the camera view. If it doesn't, you may need to adjust the [rotation and transforms](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-General-Camera-Setup#set-rotation-and-transforms) of the camera.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Actuators and Other Head Objects|Setup and Calibration: Actuators and Other Head Objects]] | [[Table of Contents|Setup and Calibration]] | [[Feeders|Setup and Calibration: Feeders]] |