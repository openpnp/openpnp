**This page is under construction**

# Purpose
The purpose of advanced camera calibration is to:
1. Determine the relative location and orientation of the camera
1. Determine the scaling of machine units to camera pixels, i.e., units per pixel and how it varies with machine Z
1. Correct for imperfections in how the camera is mounted, i.e., rotation from ideal about the X, Y and Z axis
1. Correct for lens imperfections, i.e., barrel/pincushion and tangential distortion

# Enabling/Disabling
Advanced camera calibration is enabled/disabled for a particular camera by selecting the camera in the Machine Setup tree, selecting the Experimental Calibration tab, and checking/clearing the check box "Enable experimental calibration to override old style image transforms and distortion correction" (see below). Other than the settings under the General Settings section, no other settings on this tab will have any effect on machine operation if this check box is cleared (all the settings prior to running Advance Camera Calibration will be restored):
![Experimental Calibration Tab](https://user-images.githubusercontent.com/50550971/132746491-2e46dfa1-d5bc-4f2e-bd82-7cdc7786d8e7.png)

# General Settings
![General Settings](https://user-images.githubusercontent.com/50550971/132746519-9b87e1bb-b9ff-4d80-b05c-7c1be3f0a565.png)
The settings here are simply copies of settings available on other tabs.  They are provided here as a convenience since they should be set to their desired values prior to running Advanced Calibration.  Note that any changes made here take effect immediately and will change the corresponding value on any other tabs on which they appear.

# Calibration Setup
![Calibration Setup](https://user-images.githubusercontent.com/50550971/132746566-76f7f224-9ecd-41ae-a966-f752de753103.png)
The **Primary Cal Z** and **Secondary Cal Z** settings determine the Z coordinate of the calibration test patterns that will be used to calibrate the camera.  For top cameras, these are set to the Z coordinate of the Primary and Secondary Calibration Fiducials respectively and are not editable here.  For bottom cameras, the **Primary Cal Z** is set to the Z coordinate where the nozzle tip is in best focus as determined by auto focus (not editable here) and the **Secondary Cal Z** is set by default to half of the primary setting. The operator should set this as high (more positive) as possible but yet keep the nozzle tip in reasonable focus. 

The **Radial Lines Per Cal Z** setting is used to control how many calibration data points are collected during the calibration process.  The calibration data is collected along lines that start at the center of the image and proceed out to the edge of the image.  This setting controls how many such lines are used.  Reducing this number will reduce the duration of the collection sequence but at the risk of lower quality or even failed calibration. Increasing this number will increase the duration of the collection but may result in a higher quality calibration.

The **Start Calibration** button starts the calibration collection process.  All settings on this tab above the button should be setup before clicking this. During the calibration collection process, instructions will appear below the camera view guiding the operator through the process.

# Calibration Data Collection

# Results/Diagnostics