# Top Camera Setup
The Top Camera is the camera that is attached to your machine's head. It's also sometimes referred to as the Downlooking Camera or Fiducial Camera. They all mean the same thing.

The Top Camera is used primarily when setting up feeders and jobs, and can be used to target any location on your machine. It's the thing you will look at most often when using OpenPnP.

## Add a Camera
First, add a camera by following the instructions on [[General Camera Setup|Setup and Calibration: General Camera Setup]]. Make sure to configure each section before returning to this page to continue.

## Set Camera Head
Setting the camera head tells OpenPnP that the camera is attached to the head, making it mobile, versus a machine mounted camera which is stationary.

1. Open the Cameras tab.
2. Select your camera from the table on the left.
3. Click on the Head column for your camera and select the head from the dropdown.

## Head Offsets
Camera Head Offsets tell OpenPnP where the camera is in relation to the other objects on the head, such as nozzles. It's best to use the primary camera as the origin for the head, so we set the offset to all zeros.

1. With the same camera still selected in the Camera tab, look to the right for the configuration tabs.
2. Select the tab called "Camera Specific" and you should see a section called Offsets at the top.
3. Enter all zeros for the X, Y and Z fields.
4. Press Apply.

## Camera Jogging
Now that the camera is configured, hold Shift and then Left Click within the camera view. The camera should move to the location you clicked on. This is called Camera Jogging and is the easiest way to move the machine around when setting things up.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Driver Setup|Setup and Calibration: Driver Setup]] | [[Table of Contents|Setup and Calibration]] | [[Nozzle Setup|Setup and Calibration: Nozzle Setup]] |