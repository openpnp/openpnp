# What does the Nozzle Tip Calibration do?
Using the bottom camera and computer vision the position of the nozzle tip held above the camera can be precisely determined. By moving the nozzle tip in certain patterns, OpenPNP can calibrate the following things:
1. The runout of the nozzle and nozzle tip.
2. The true location of the bottom camera i.e. where a nozzle tip has to be moved to, in order to appear precisely in the center of the camera view.
3. The true rotation of the bottom camera so that the nozzle tip moves precisely horizontal in X (and precisely vertical in Y).

# What is Runout and Runout Compensation?
See the animations below. Left side is without compensation, right side is compensation enabled. The compensation algorithm removes the eccentricity of the nozzle tip to gain better placement accuracy. See the difference with a 3d printed demo-nozzle tip:

![nozzle tip without compensation](https://user-images.githubusercontent.com/3868450/52703638-161f1a80-2f7f-11e9-8e5d-746018b3beb5.gif)
![runout compensated](https://user-images.githubusercontent.com/3868450/52703637-161f1a80-2f7f-11e9-9e15-68b40c6df9f4.gif)

Runout can come from both the nozzle tip or the nozzle it is attached to. The calibration system will automatically handle any combination of the two.

# What is the Bottom Camera Location and how can it vary between nozzles?
In order to be able to precisely align parts in Bottom Vision, the true location of the bottom camera in relation to the nozzle tip must be known. For a first rough setup, [the location of the bottom camera must be set up manually](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Bottom-Camera-Setup#set-camera-location).

However it can be quite difficult to set the _precise_ center of the nozzle tip using the naked eye, especially if the nozzle tip or the nozzle has runout. While the first location setup needs to be done manually, the Nozzle Tip Calibration can automatically improve that for you. 

For a multi-nozzle machine, things might even get a bit more complicated, especially if your bottom camera does not have a focal plane that exactly matches the surface plane of the PCB. The nozzle Z axes might not be perfectly parallel and can introduce a slight offset. The following exagerated illustration shows how this might happen:

![Z axes parallax](https://user-images.githubusercontent.com/9963310/58963177-fff6cd80-87ac-11e9-8bd8-fc7c83e75151.png)

While the bottom camera location can be set up perfectly for the first nozzle, there might be an offset needed for the other nozzles. The Nozzle Tip Calibration can automatically calibrate and apply this offset whenever it is needed. 

# What is Bottom Camera Rotation and how does it matter?

The Bottom Camera (or even the sensor chip inside) might be mounted with a slight rotation in relation to the X and Y axes of the machine. When aligning large parts, even a slight rotation will introduce surprisingly large placement errors.

The rotation can be [compensated using the Camera Rotation](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-General-Camera-Setup#set-rotation-and-transforms). However this is quite hard to do manually using just the jog and trial and error. The Nozzle Tip Calibration can automatically calibrate and apply this rotation.

# Preparation
The calibration can only work correctly if the location of the nozzle tip in relation to the machine coordinate system is precisely known. OpenPNP uses the down-looking camera location as the coordinate reference. So it is very important to set up the down-looking camera to nozzle offset correctly, before you start using the calibration. 

Use the [Nozzle Offset Wizard](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Nozzle-Setup#head-offsets) to set up the offset first. Be sure to rotate the nozzle tip in the marker material to get the true nozzle axis center, regardless of any runout. 

# Basic Calibration Setup
You can setup the calibration features per nozzle tip. This allows you to tune the pipeline well to the different nozzle tips' diameter sizes. 
1. Enable the feature for each nozzle tip you have issues with runout. If you have a multi-nozzle machine with potential camera focal plane offsets, you need to enable it for all the tips.
2. The calibration algorithm needs measurements of the nozzle tip location at different angles to calculate the runout and offsets. You may set the circle divisions as low as 3, making 3 measurements at -180°, -60° and 60°. For higher accuracy you may want to choose 6. 
3. Set the allowed misdetects, so the calibration becomes more robust and won't interrupt a job just because of a single calibration misdetect. At least 3 measurements must remain.  
4. The calibration will use the Offset Threshold as a simple criteria to remove obviously invalid measurements from the computer vision's results when they are too far away from the center. Choose a threshold value slightly higher than the max. runout plus offset you expect for the tip.   
5. Now move the nozzle tip to the bottom camera using the tool to camera button.
6. Edit the pipeline. Best is a pipeline that returns only exactly one result. You can use DetectHoughCircles and SimpleBlobDetector to find the nozzle tip.
7. Click apply.
8. Click calibrate.
9. When the process is finished you may want to check that the nozzle tip is well centered over the bottom camera. Click the tool to camera button again and rotate the nozzle.
10. If you're satisfied that the tip center always stays precisely in the crosshairs of the camera, you're set.
11. The Status text will inform you how large the runout was, that is now compensated. If it is significant, you might now want to repeat the [Nozzle Offset Wizard](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Nozzle-Setup#head-offsets) with the calibrated nozzle tip. No need to rotate the nozzle in the material this time. You will get a smaller mark and a more precise offset capture. Note that changing the nozzle offset will auomatically invalidate any nozzle tip calibrations. Simply repeat the calibration afterwards.

![Nozzle Tip Calibration Wizard](https://user-images.githubusercontent.com/9963310/59954597-da90e180-9485-11e9-8560-8157bdaa5453.png)

# Bottom Camera Calibration
Once you have your nozzle tip calibrated precisely, you can use the "Calibrate Camera Position and Rotation" button. OpenPNP will rotate the nozzle tip in a wide circle to not only get the precise center, but also the rotation of the camera in relation to the X and Y axes. 

![Rotating the nozzle tip in a large circle.](https://user-images.githubusercontent.com/9963310/56699028-2f1d1800-66f4-11e9-9b77-4a75719ee138.gif)

This is only needed for the first or "best" nozzle and nozzle tip. If the machine has multiple nozzles, the others will automatically be compensated for any camera focal offsets by using the calibration system. 

# Advanced Calibration Setup
## Calibration Z Offset
On some nozzle tips the feature best captured by computer vision may be a bit up on the nozzle tip. For instance a large cup shaped nozzle tip might have an air hole receded quite a bit. For best precision and focus you can compensate that with the Calibration Z Offset.

## Automatic Recalibration
OpenPNP will automatically recalibrate the nozzle tips as needed. The following triggers can be defined:
* On Each Nozzle Tip Change: Whenever a nozzle tip is changed, it is immediately calibrated. This also happens when the machine is homed, and a nozzle tip is already loaded.
* On Each Nozzle Tip Change in a Job: Whenever a nozzle tip is changed inside a job, it is calibrated. This also happens when a nozzle tip was already loaded. However no automatic calibration is done outside the job. 
* On Machine Homing: When the machine is homed, all the currently loaded nozzle tips are calibrated. When other nozzle tips are later loaded for the first time, they are also calibrated. However once a tip is calibrated it will retain the calibration through all unload/load operations. 
* Manual: Nozzle tips are only calibrated manually. The calibration is stored in the machine.xml and will be reused even if OpenPNP was closed and restarted. This only works correctly for machines that have homed rotation axes. 

## Calibrating the Bare Nozzle
For machines that have very large runout in the nozzle it might be imperative that they can also runout compensate the bare nozzle e.g. to safely and smoothly load new nozzle tips. There is a simple trick in OpenPNP to do that: just create a nozzle tip that is named "unloaded". The calibration system will automatically use this stand-in whenever the bare nozzle needs to be calibrated and compensated. 

On the "unloaded" nozzle tip's Calibration Wizard, setup the pipeline and other settings as usual. Use the Calibration Z Offset to account for the missing tip on the nozzle and to bring it down into the focal plane of the bottom camera. OpenPNP will safely suppress any changer motion for this pseudo nozzle tip. 

## Pipeline
The pipeline should detect the nozzle tip in a very stable way. Furthermore it should best return only exactly ONE result, not many. Here are suggestions how to adapt the default pipeline for your needs. Its not every aspect of the pipelines described here. Just parts that might be helpful for nozzle detection. Find the following animation of a successful measurement with the detected houghcircles overlaid:

![calibration-working](https://user-images.githubusercontent.com/3868450/52703639-16b7b100-2f7f-11e9-9c2d-d4e9d95464d2.gif)


### Juki-Nozzles

  * Threshold

Adapt the Threshold just as high, that the nozzle tip shows as circle and doesn't bleed out.
![Threshold](https://user-images.githubusercontent.com/3868450/51399985-8c2e8e00-1b47-11e9-9cf0-c20e6b3cf8ad.PNG)

  * DetectCirclesHough

INFO INFO. 
Most relevant parameters: threshold value, mask circle diameter, houghcircle diameter max/min
![DetectCirclesHough](https://user-images.githubusercontent.com/3868450/51399987-8c2e8e00-1b47-11e9-8c37-0f9d9148300d.PNG)

  * If you were successful, it should look like this in the recall

![pipeline3](https://user-images.githubusercontent.com/3868450/51399984-8c2e8e00-1b47-11e9-92df-b6ac2bddb79b.PNG)


### Samsung CP40
These nozzle tips are low contrast black on black. Not easy. Be sure to set your camera to manual exposure for repeatable results. Use the air hole as the detection feature as it tends to be the darkest area. Set the threshold so only the hole is masked. Increase the MedianBlur kernel size for more noise removal. Set the Hough Circle min/max diameter only a few pixels below/above the true hole diameter. Use a min distance similar to the hole diameter. Use a tight MaskCircle to remove any peripheral dark edged. Note that while editing/testing the pipeline you can still operate the machine controls, so be sure to rotate the nozzle and refresh the image many times, testing the pipeline performance. 
