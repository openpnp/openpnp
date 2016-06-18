# General Camera Setup

## Add a New Camera

1. Add a camera on the "Cameras" tab by pressing the green plus button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/file-add.svg).  
2. Select a camera driver from the provided list, try the "OpenCVCamera" and press the "Accept" button. The newly added camera will show up in the camera list.
3. Double click on the name of the new camera to rename it.  
4. Click on the "Camera Specific" tab (while your newly created camera is still selected in the camera list).
5. In the "General" section you can pick the "Device Index".  Each camera connected to your computer will have a unique device index starting at index 0. 
6. Verify your camera is working, in the "Camera" window select your newly added camera from the drop down list.  If configured correctly you should see a live image from your selected camera.

## Set Rotation and Transforms


## Set Units Per Pixel


## Lens Calibration

Lens calibration can be used to remove lens distortion from your camera. It can also remove the fisheye effect from short lenses. An easy way to tell if you need lens calibration is to hold a piece of graph paper in front of it and see if the lines look curved in the image. If they do, check out [[Camera Lens Calibration]] to learn how to correct it.
