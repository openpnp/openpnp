# Top Camera Setup
The Top Camera is the camera that is attached to your machine's head. It's also sometimes referred to as the Downlooking Camera or Fiducial Camera. They all mean the same thing.

The Top Camera is used primarily when setting up feeders and jobs, and can be used to target any location on your machine. It's the thing you will look at most often when using OpenPnP.

## Add a Camera
First, add a head camera by following the instructions in [[General Camera Setup|Setup and Calibration: General Camera Setup]]. Make sure to configure each section before returning to this page to continue.

## Head Offsets
Camera Head Offsets tell OpenPnP where the camera is in relation to the other objects on the head, such as nozzles. It's best to use the primary camera as the origin for the head, so we set the offset to all zeros.

![screen shot 2016-12-18 at 8 37 41 pm](https://cloud.githubusercontent.com/assets/1182323/21299497/e30e6d82-c561-11e6-8f42-1663c2f994d0.png)

1. With the same camera still selected in the Machine Setup tab, look to the right for the configuration tabs.
2. Select the tab called "Camera Specific" and you should see a section called Offsets at the top.
3. Enter `0.0` for each of the X, Y and Z fields.
4. Press Apply.

## Camera Jogging
Now that the camera is configured, click and drag anywhere in the camera view. You'll see a white line follow the mouse cursor. When you release the mouse button the camera should move to the location you were pointing at. This is called Camera Jogging and is the easiest way to move the machine around when setting things up. You can also hold Shift and then Left Click within the camera view to do the same thing.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Driver Setup|Setup and Calibration: Driver Setup]] | [[Table of Contents|Setup and Calibration]] | [[Steps Per Mm|Setup and Calibration: Steps Per Mm]] |