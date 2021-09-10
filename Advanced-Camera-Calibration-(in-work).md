**This page is under construction**

# Purpose
The purpose of advanced camera calibration is to:
1. Determine the relative location and orientation of the camera;
1. Determine the scaling of machine units to camera pixels, i.e., units per pixel and how it varies with machine Z;
1. Correct for imperfections in how the camera is mounted, i.e., rotation from ideal about the X, Y and Z axis; and,
1. Correct for lens imperfections, i.e., barrel/pincushion and tangential distortion.

# Enabling/Disabling
Advanced camera calibration is enabled/disabled for a particular camera by selecting the camera in the Machine Setup tree, selecting the Experimental Calibration tab, and checking/clearing the check box "Enable experimental calibration to override old style image transforms and distortion correction" (see below). Other than the settings under the General Settings section, no other settings on this tab will have any effect on machine operation if this check box is cleared (all the settings prior to running Advance Camera Calibration will be restored):
![Experimental Calibration Tab](https://user-images.githubusercontent.com/50550971/132746491-2e46dfa1-d5bc-4f2e-bd82-7cdc7786d8e7.png)

# General Settings
![General Settings](https://user-images.githubusercontent.com/50550971/132746519-9b87e1bb-b9ff-4d80-b05c-7c1be3f0a565.png)
The settings here are simply copies of settings available on other tabs.  They are provided here as a convenience since they should be set to their desired values prior to running Advanced Calibration.  Note that any changes made here take effect immediately and will change the corresponding value on any other tabs on which they appear.

# Calibration Setup
![Calibration Setup](https://user-images.githubusercontent.com/50550971/132746566-76f7f224-9ecd-41ae-a966-f752de753103.png)
The **Primary Cal Z** and **Secondary Cal Z** settings determine the Z coordinate of the calibration test patterns that will be used to calibrate the camera.  For top cameras, these are set to the Z coordinate of the Primary and Secondary Calibration Fiducials respectively and are not editable here.  For bottom cameras, the **Primary Cal Z** is set to the Z coordinate where the nozzle tip is in best focus as determined by auto focus (not editable here) and the **Secondary Cal Z** is set by default to half of the primary setting. The operator should set this as high (more positive) as possible but still keeping the nozzle tip in reasonable focus. 

The **Radial Lines Per Cal Z** setting is used to control how many calibration data points are collected during the calibration process.  The calibration data is collected along lines that start at the center of the image and proceed out to the edge of the image.  This setting controls how many such lines are used.  This setting gets rounded up to the next multiple of four.  Reducing this number will reduce the duration of the collection sequence but at the risk of lower quality or even failed calibration. Increasing this number will increase the duration of the collection but may result in a higher quality calibration.

The **Start Calibration** button starts the calibration collection process.  All settings on this tab above the button should be setup before clicking this. During the calibration collection process, instructions will appear below the camera view guiding the operator through the process.

The **Detection Diameter** spinner sets the size of the fiducial/nozzle tip that is used.  During the calibration sequence, the operator will be instructed to set this at the appropriate time.

Once the calibration data has been collected and processed, the **Apply Calibration** check box will automatically be selected thereby applying the calibration to the camera and enabling calibrated images to be displayed in the Camera View.  Clearing this checkbox, results in raw images being displayed in the Camera View.

The slider at the bottom of this section allows the operator to control the cropping of invalid pixels along the edges of the calibrated camera images.  While mostly aesthetic, changing this setting does change the Units Per Pixel for the camera.  Setting the slider to the far right ensures all available pixels from the camera are displayed.  Setting the slider to the far left ensures that the edges of the image are cropped symmetrically about the reticle crosshairs.

# Calibration Data Collection
Upon clicking the **Start Calibration** button, the calibration proceeds as follows:

## Top Cameras
1. The operator is requested to jog the camera so that the Primary Calibration Fiducial is centered.
1. The operator is requested to adjust the Detection Diameter spinner to achieve stable detections on the fiducial.
1. The camera is moved in a couple of small steps to obtain an estimate of the camera's units per pixel at the Z coordinate of the Primary Calibration Fiducial.
1. The camera is moved so that the Primary Calibration Fiducial is exactly centered.
1. The camera is moved in steps along radial lines so that the fiducial moves from the center of the image to the edge of the image. The radial lines are equal spaced angularly about the center but are visited in a random order. As each line is completed, the camera returns to the 
1. Steps 1 - 5 are repeated with the Secondary Calibration Fiducial.
1. The calibration data is processed, and if successful, the results are applied.
 
## Bottom Cameras

# Results/Diagnostics