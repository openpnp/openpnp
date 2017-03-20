# General Camera Setup

## Add a New Camera

1. Open the Machine Setup tab.
2. 
    * If you are adding a head mounted camera, find the head in the tree on the left. Under the head look for Cameras and select it.
    * If you are adding a machine mounted camera, find Cameras under the root of the tree and select it.
3. Add a camera by pressing the green plus button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/general-add.svg).  
2. Select a camera driver from the provided list, try "OpenCVCamera" (USB) or "OnvifIPCamera" (IP) and press the "Accept" button. The newly added camera will show up in the camera list.
3. Click on the name of the new camera to open it's properties.
4. Click on the "Camera Specific" tab.
5. In the "General" section you can configure settings specific to the type of camera (USB or IP).
 - OpenCVCamera: Set the "USB Device Index".  Each camera connected to your computer will have a unique device index starting at index 0.
 - OnvifIPCamera: Set the "Camera IP" as &lt;IP address&gt;:&lt;port&gt;, "Username", and "Password" (optional).
 - Press the "Apply" button to have your changes applied.
6. Verify your camera is working, in the "Camera" window select your newly added camera from the drop down list.  If configured correctly you should see a live image from your selected camera.
7. On the General Configuration tab click the name field in the camera table to give your camera a descriptive name. We suggest "Top Camera" and "Bottom Camera", respectively.

## Camera Type Specific Setup

[[OpenCVCamera]] - USB cameras  
[[OnvifIPCamera]] - IP (network) cameras

## Set Rotation and Transforms

When you look at the camera image in OpenPnP it should be right side up. Depending on how your camera is mounted, this might not be the case. By setting transformations in the camera configuration you can adjust the image so it appears correct.

A Top Camera image should appear as if you are looking down at the machine. The top of the image should be away from you, the bottom of the image should be towards you.

A Bottom Camera image should appear as if you are looking up from the camera towards the nozzle. The same rules as above apply. 

1. Open the Machine Setup tab and select the camera from the tree.
2. Select the Camera Specific tab from the configuration tabs on the right.
3. Scroll to the Transformation section.
4. Adjust the Rotation, Flip Vertical and Flip Horizontal fields to make the image appear as described above. Press Apply to have your settings show up in the camera view.
5. Press Apply before moving on.

## Set Units Per Pixel

Units Per Pixel is how OpenPnP maps pixels to real world units, typically millimeters or inches. OpenPnP needs to know how large a pixel is in real world terms so that it can measure things correctly when performing computer vision. This is the most important configuration for a camera, so it's worth taking the time to get it right.

1. With the camera selected in the Machine Setup tab, select the General Configuration tab from the right.
2. Look for the Units Per Pixel section and read the instructions contained within.
3. Make sure to enter the real world width and height of the object you are measuring into the Width and Height fields before clicking Measure.
4. After you click Confirm, the X and Y fields should be updated with the measured values.
5. Click Apply to save your data.

Once the Units Per Pixel is set you can test the results.

1. Place a ruler on the bed of the machine and position the camera over it.
2. Right click the camera view to bring up the camera menu.
3. Select Reticle -> Ruler.
4. In the same menu, select Reticle -> Options -> Units and Reticle -> Options -> Units Per Tick to configure the on screen ruler with the same units and size as your actual ruler.

There should now be a crosshair with evenly spaced lines in the camera view. Line up the center of the crosshair with one of the lines on your ruler and the rest of the lines should line up closely. It's okay if they don't line up perfectly as they get to the edges of the image but the lines in the center half of the image should be quite close to the lines on the ruler.

If the lines don't line up, check that you've performed this step correctly. If they do line up in the center but rapidly get worse the further you look from center, read the Lens Calibration section below to perform correction.

## Settle Time

Settle time is the amount of time it takes for your camera to adjust to what it's looking at before a picture can be taken. This usually takes into account any blur from the end of a movement, along with time needed to perform any auto exposure or focus operations. The default settle time is 250 milliseconds. If you find that things like fiducial checks are getting blurry images, you may need to increase the settle time.

1. With the camera selected in the Machine Setup tab, select the General Configuration tab from the right.
2. In the configuration panel, look for Settle Time (ms) and change the value to the number of milliseconds required. The easiest way to determine the value is to start high (2000ms) and then lower it until you stop getting good results and use a slightly higher value.

## Lens Calibration

Lens calibration can be used to remove lens distortion from your camera. It can also remove the fisheye effect from short lenses. An easy way to tell if you need lens calibration is to hold a piece of graph paper in front of it and see if the lines look curved in the image. If they do, check out [[Camera Lens Calibration]] to learn how to correct it.

