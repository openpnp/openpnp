# General Camera Setup

## Convert Simulation Cameras

When you first start with OpenPnP, it contains two simulated cameras, so you can play around with a "virtual machine". Once you want to go ahead and use real cameras, you can use the [[Issues and Solutions]] system to automatically convert these cameras to real USB cameras i.e. OpenPnpCaptureCameras. The system will then keep some settings (e.g. axis assignments) in order. 

If using [[Issues and Solutions]] you can **skip the following two sections**. 

## Add a New Camera

1. Open the Machine Setup tab.
2. 
    * If you are adding a head mounted camera (down looking, fiducial), find the head in the tree on the left. Under the head look for Cameras and select it.
    * If you are adding a machine mounted camera (up looking, bottom vision), find Cameras under the root of the tree and select it.
3. Add a camera by pressing the green plus button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/general-add.svg).  
2. Select a camera driver from the provided list. Most users should use "OpenPnpCaptureCamera". This driver supports multiple cameras on a single USB host and works best for most installations. For other situations, try "OpenCVCamera" (USB), "OnvifIPCamera" (IP) etc. and press the "Accept" button. The newly added camera will show up in the camera list.
3. Click on the name of the new camera to open its properties.
4. Click on the "Camera Specific" tab and scroll down to the "General" section. Here you can configure settings specific to the type of camera (USB or IP).
    * OpenPnpCaptureCamera: Select the camera from the "Device" dropdown, then a format from the "Format" dropdown. **Important:** If you will be using more than one camera, select a "1bmd" or "mjpg" format and not a "yuv", "yuv2" or "2vuy" format. For more information see [[OpenPnpCaptureCamera]].
    * OpenCVCamera: Set the "USB Device Index".  Each camera connected to your computer will have a unique device index starting at index 0.
    * OnvifIPCamera: Set the "Camera IP" as &lt;IP address&gt;:&lt;port&gt;, "Username", and "Password" (optional).
    * Press the "Apply" button to have your changes applied.
5. Verify your camera is working, in the "Camera" window select your newly added camera from the drop down list.  If configured correctly you should see a live image from your selected camera.
6. On the General Configuration tab click the name field in the camera table to give your camera a descriptive name. We suggest "Top Camera" and "Bottom Camera", respectively.

## Delete Old Camera

When you first install OpenPnP it comes with a set of defaults that simulate a machine so that you can try it out. When you start to set up OpenPnP for your real machine, you need to remove some of those defaults so they don't interfere with your machine.

If you are adding a Top Camera, look to see if there is an existing Top Camera called ImageCamera. Delete it.

For the Bottom Camera, you may see an existing one called SimulatedUpCamera. Delete it.

## General Configuration

In addition to the already mentioned **Name** and **Looking** properties, some additional settings can be configured:

![General Configuration](https://user-images.githubusercontent.com/9963310/113151036-ff0e1600-9234-11eb-97bf-f457c4f04eef.png)

**Preview FPS** (frames per second) is universally available for all the camera implementations (if using a SwitcherCamera please use a reasonably low FPS). Note, you can set fractional FPS like **0.5** for an update every 2 seconds. Another important option is to set **0 FPS**, where only frames explicitly captured for computer vision or for other deliberate user camera actions are shown in the Camera View. This is the most efficient setting, optimal for slow computers and/or when you use many cameras. 

**Suspend during tasks?** allows you to use a relatively high **Preview FPS** during manual machine control, while effectively setting it to the efficient **0 FPS** during machine tasks, especially during Jobs. This is also the only reasonable setting for a SwitcherCamera with preview. 

**Auto Camera View?** automatically selects the active camera in the Camera View (if another single camera was selected before). This happens on deliberate user camera actions such as positioning, jogging the camera or when computer vision captures frames or displays marked-up result images. 

**Show in multi camera view** (enabled by default) determines whether the camera is shown/not shown in multi camera view panels (Show All Horizontal/Show All Vertical). This is typically used to hide the capture device in a SwitcherCamera setup. The Camera will still be selectable as a single CameraView.
  
   ![Show All](https://user-images.githubusercontent.com/9963310/106962435-6761df00-673f-11eb-8d8e-4098cbacb094.png)

**Notes:** When both  **Suspend during tasks?** and **Auto Camera View?** are enabled, there will only be clean result images presented during a job. By only displaying one camera at a time, you get a larger preview with much better resolution. **Tip:** make sure to right-click the camera View and choose **High** or **Highest** quality rendering. 

**Focus Provider**: The bottom camera can optionally enable Auto-Focus capabilities. See the [[Up looking Camera Auto Focus]] page.

## Camera View Configuration

Check out the context menu in the Camera View. You can set various options there:

![Camera View Options](https://user-images.githubusercontent.com/9963310/105194658-1bf1e300-5b3a-11eb-9391-2bb9092a60a2.png)

## Camera Type Specific Setup

* [[OpenPnpCaptureCamera]] - USB Cameras (Recommended)
* [[OpenCVCamera]] - USB Cameras (Not Recommended)
* [[OnvifIPCamera]] - IP (Network) cameras

## Lens Calibration

Lens calibration can be used to remove lens distortion from your camera. It can also remove the fisheye effect from short lenses. An easy way to tell if you need lens calibration is to hold a piece of graph paper in front of it and see if the lines look curved in the image. If they do, check out [[Camera Lens Calibration]] to learn how to correct it and then come back to this page to continue the rest of the setup.

## Set Rotation and Transforms

When you look at the camera image in OpenPnP it should be right side up. Depending on how your camera is mounted, this might not be the case. By setting transformations in the camera configuration you can adjust the image so it appears correct.

A Top Camera image should appear as if you are looking down at the machine. The top of the image should be away from you, the bottom of the image should be towards you.

A Bottom Camera image should appear as if you are looking onto a mirror laying on the floor reflecting a view up from the camera towards the nozzle. Again the top of the image should be away from you, the bottom of the image should be towards you. But because left and right should remain left and right (and it is actually a mirror image), you usually need to enable either Flip Vertical or Flip Horizontal depending on how you mounted the camera. If you need to make adjustments:

1. Open the Machine Setup tab and select the camera from the tree.
2. Select the Camera Specific tab from the configuration tabs on the right.
3. Scroll to the Transformation section.
4. Adjust the Rotation, Flip Vertical and Flip Horizontal fields to make the image appear as described above. Press Apply to have your settings show up in the camera view.
5. Press Apply before moving on.

See the GIF below for an example of how the camera view should react after correct setup:

![0](https://user-images.githubusercontent.com/1182323/42544960-72138ffc-847a-11e8-8477-8b07f965fc41.gif)


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

## Computer Vision and Camera Settling

Settle time is the amount of time it takes for your camera to adjust to what it's looking at before a picture can be taken for Computer Vision. 

The Setup is described on the [[Camera Settling]] page.

