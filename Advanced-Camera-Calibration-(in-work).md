**This page is under construction**

# Purpose
The purpose of advanced camera calibration is to:
1. Determine the head offsets (or absolute location for bottom cameras) and orientation of the camera;
1. Determine the scaling of machine units to camera pixels, i.e., units per pixel and how it varies with machine Z;
1. Correct for imperfections in how the camera is mounted, i.e., rotation from ideal about the X, Y and Z axis; and,
1. Correct for lens imperfections, i.e., barrel/pincushion and tangential distortion.

# Enabling/Disabling
Advanced camera calibration is enabled/disabled for a particular camera by selecting the camera in the Machine Setup tree, selecting the Experimental Calibration tab, and checking/clearing the check box "Enable experimental calibration to override old style image transforms and distortion correction" (see below). Other than the settings under the General Settings section, no other settings on this tab will have any effect on machine operation if this check box is cleared (all the settings prior to running Advance Camera Calibration will be restored):
![Experimental Calibration Tab](https://user-images.githubusercontent.com/50550971/134405655-af1e0392-208b-47e2-86a5-38b05bbca6af.png)

# General Settings
![General Settings](https://user-images.githubusercontent.com/50550971/134407187-2b6fec41-4ef0-449c-b8b4-11a1b6394ff7.png)

The settings here are simply copies of settings available on other tabs.  They are provided here as a convenience since they should be set to their desired values prior to running Advanced Calibration.  Any changes made here take effect immediately and will change the corresponding value on any other tabs on which they appear.

Note that **Cropped Width** and **Cropped Height** are applied to the raw camera images prior to any other corrections made by this calibration.  Therefore, calibration data is only collected over the cropped image and any corrections will only be valid over that portion of the image. 

# Calibration Setup
![Calibration Setup](https://user-images.githubusercontent.com/50550971/134420670-04849671-d67a-4606-8834-409b9f57b74b.png)

The **Primary Cal Z** and **Secondary Cal Z** settings determine the Z coordinate of the calibration test patterns that will be used to calibrate the camera.  For top cameras, these are set to the Z coordinate of the Primary and Secondary Calibration Fiducials respectively and are not editable here.  For bottom cameras, the **Primary Cal Z** is set to the Z coordinate where the nozzle tip is in best focus as determined by auto focus (not editable here) and the **Secondary Cal Z** is set by default to half of the primary setting. The operator should set this as high (more positive) as possible but still keeping the nozzle tip in reasonable focus. 

The **Radial Lines Per Cal Z** setting is used to control how many calibration data points are collected during the calibration process.  The calibration data is collected along lines that start at the center of the image and proceed out to the edge of the image.  This setting controls how many such lines are used.  To keep things symmetrical, this setting gets rounded up to the next multiple of four.  Reducing this number will reduce the duration of the collection sequence but at the risk of lower quality or even failed calibration. Increasing this number will increase the duration of the collection but may result in a higher quality calibration.

The **Approximate Camera Lens Z** setting is used to give the calibration algorithms a hint at the Z coordinate of the camera lens. Since this is only a hint, a rough approximation (within +/- 20 percent) is generally sufficient. A rough ruler measurement from an object of known Z such as the top surface of a PCB to the camera lens is all that is required. For top cameras add the ruler measurement to the object's Z coordinate and for bottom cameras, subtract the ruler measurement.

The **Start Calibration** button starts the calibration collection process.  All settings on this tab above the button should be setup before clicking this. During the calibration collection process, instructions/status will appear below the camera view guiding the operator through the process:
![Instructions/Status](https://user-images.githubusercontent.com/50550971/134715025-15d9b20d-ef11-4ad6-b3ec-34d5fdf1e4a5.png)

The **Detection Diameter** spinner sets the size of the fiducial/nozzle tip that is used.  During the calibration sequence, the operator will be instructed to set this at the appropriate time.

Once the calibration data has been collected and processed, the **Apply Calibration** check box will automatically be selected thereby applying the calibration to the camera and enabling calibrated images to be displayed in the Camera View.  Clearing this checkbox, results in raw images being displayed in the Camera View.

The **Crop All Invalid Pixels <--> Show All Valid Pixels** slider at the bottom of this section allows the operator to control the cropping of invalid pixels along the edges of the calibrated camera images.  Invalid pixels (usually displayed as black) on the edges of the image may result due to the image processing that compensates for errors in camera mounting and lens distortion. While mostly aesthetic, changing this setting does change the Units Per Pixel for the camera.  Setting the slider to the far right ensures all available pixels from the camera are displayed.  Setting the slider to the far left ensures that the edges of the image are cropped symmetrically about the reticle crosshairs so that no invalid pixels remain at the edges. Intermediate settings will produce results between the two extremes.

## Calibration Data Collection
Upon clicking the **Start Calibration** button, the calibration proceeds as follows:

### Top Cameras
1. The operator is requested to jog the camera so that the Primary Calibration Fiducial is centered.
1. The operator is requested to adjust the Detection Diameter spinner to achieve stable detections on the fiducial.
1. The camera is moved in a couple of small steps to obtain an estimate of the camera's units per pixel at the Z coordinate of the Primary Calibration Fiducial.
1. The camera is moved so that the Primary Calibration Fiducial is exactly centered.
1. The camera is moved in steps along radial lines so that the fiducial moves from the center of the image to the edge of the image. The radial lines are equally spaced angularly about the center but are visited in a random order. As each line is completed, the camera is moved to bring the fiducial back to the center of the image so that it is ready to begin the next line. 
1. Steps 1 - 5 are repeated with the Secondary Calibration Fiducial.
1. The calibration data is processed, and if successful, the results are applied.
 
### Bottom Cameras
1. The operator is requested to load the smallest nozzle tip.
1. The operator is requested to jog the nozzle tip so that it is approximately centered in the camera view.
1. The nozzle tip is moved to the Primary Calibration Z and the operator is requested to verify that the nozzle tip is still centered.
1. The operator is requested to adjust the Detection Diameter spinner to achieve stable detections on the nozzle tip.
1. The nozzle tip is moved in a couple of small steps to obtain an estimate of the camera's units per pixel at the Primary Calibration Fiducial.
1. The nozzle tip is moved so that it is exactly centered.
1. The nozzle tip is moved in steps along radial lines so that it moves from the center of the image to the edge of the image. The radial lines are equally spaced angularly about the center but are visited in a random order. As each line is completed, the nozzle tip is moved to bring it back to the center of the image so that it is ready to begin the next line. 
1. Steps 3 - 7 are repeated but at the Secondary Calibration Z
1. The calibration data is processed, and if successful, the results are applied.

# Results/Diagnostics
![Results/Diagnostics](https://user-images.githubusercontent.com/50550971/134698536-0adf308e-d3b0-4380-8958-9a1c0e263292.png)

The first item displayed here is the **Head Offsets** (for top cameras) or the **Camera Location** (for bottom cameras). For the default top camera, the head offsets displayed here are just a mirror of what was entered on the Position tab. If multiple top cameras are mounted on a head, the default top camera must be calibrated first as the head offsets of all other top cameras are calculated relative to the default camera's head offsets. For bottom cameras, the location displayed here is the X-Y coordinates of the physical camera and Z is the Z coordinate determined by auto focus.

**Units Per Pixel** displays the computed units per pixel at default Z for this camera. Note that images are scaled such that the units per pixel is the same in both the X and Y directions.

**Camera Mounting Error** shows the estimated mounting errors of the camera as measured by the right hand rule about each machine axis. Look at this to see how mounting errors effect the images as seen in the camera view pane.  
