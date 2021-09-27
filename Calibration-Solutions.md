# What is it?

Calibration Solutions are provided by [[Issues and Solutions]] when targeting the **Calibration** milestone. This page as a whole provides the background information for it. Individual sections are linked from the corresponding solutions.  

The Calibration Solutions are part of an overall holistic (whole-machine) approach to calibrate both the cameras and various mechanics of the machine by playing the two against each other. On one hand, the known metrics of machine motion are used to calibrate the cameras. On the other hand, the camera is used to make the motion more precise. The inherent chicken and egg situation between the two is circumvented by exploiting various forms of symmetry and by approximation through iteration, things that would be hard or tedious to do by hand. 

To get some impressions, [watch a video of these and other calibration solutions](https://youtu.be/md68n_J7uto). The video is not complete, so come back to this page.

# Calibrating Backlash Compensation

Backlash compensation is used to avoid the effects of any looseness or play in the mechanical linkages of machine axes. Using the camera, looking at the calibration primary fiducial, a special calibration process can automatically determine the backlash of the machine X and Y axes. It can therefore configure [[Backlash Compensation]] with the correct method, speed and offsets to compensate the errors. 

**NOTE:** on mechanically superior machines, backlash compensation will automatically be switched off if it is not needed. So even if you are convinced that your machine does not need it, it makes sense to perform this calibration and confirm everything works as expected. You'll also get a ton of diagnostic information in the process (see below).

![Issues and Solutions](https://user-images.githubusercontent.com/9963310/134890820-97381189-ce8b-4e3d-9bb3-f78b9c5f6107.png)

**Tolerance ±** can be used to set the wanted tolerance for the axis. The calibration routine may be able to select a more performant method if the granted tolerance is greater. More performant methods avoid direction changes for a more fluid and faster overall machine performance. The calibration routine will do its best to aim for the wanted tolerance, but no guarantee can be given. 

More information about the rationale and the methods used etc. can be found on the [[Backlash Compensation]] page. 

## Preparing for Backlash Calibration

The driver must first be properly configured in terms of [Motion Control Type](https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings). One of the advanced motion control types must be selected, i.e. **ConstantAcceleration** or better, where OpenPnP can effectively control the speed factor, i.e. not just the feed-rate, but also the acceleration (and optionally jerk). The calibration needs to perform elaborate measurements using different speed factors, otherwise the routine is useless or even misguiding. 

You also need to make sure your [Kinematic Axis Rate Limits](https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--rate-limits) are properly set and tuned to the maximum. Backlash will typically change when you make an axis accelerate (or jerk) harder. Therefore, you need to tune the axes' rate limits first, and/or repeat the backlash calibration again, after changing them. 

## Performing the Backlash Calibration

After having prepared everything, press the **Accept** button to perform the calibration. **NOTE:** the routine will test many aspects of the axis behavior, this will take many minutes. 

![Machine disable](https://user-images.githubusercontent.com/9963310/134893587-54c535b2-b019-4679-a8d7-ccc29ab505ed.png) Press the **Machine Disable** button, if you want to interrupt it. 

After the calibration has completed, go to the axis to see the diagnostics.

## Backlash Calibration on the Axis / Diagnostics

The calibration function is also available on the Axis, and elaborate graphical diagnostics are available there:

![Backlash Wizard](https://user-images.githubusercontent.com/9963310/134782398-0faaac87-1cbf-4c15-9f02-4bfb6ddb9ccb.png)



# Calibrating Precision Camera to Nozzle Offsets

To calibrate precision camera ↔ nozzle offsets, we let the nozzle pick, rotate and place a small test object and then measure the result using the camera. By exploiting the effects of rotational symmetry and averaging, the precise nozzle axis can be determined. 

The test object should be circular and flat on the top. The nozzle must be able to pick and place it precisely. There must be good contrast between test objects and background. As you are likely to lose or damage these little test objects, make many. They can be as simple as punched out "confetti" from a hole punch, used on matte card stock. Press them flat. Don't worry if they are not perfect, this will all be cancelled-out through symmetry. 

![Hole Punch Confetti](https://user-images.githubusercontent.com/9963310/119668622-a1aed380-be37-11eb-97cc-a99f7220ea04.jpg)
