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

Failure to prepare properly will be reported with this Error:

![Error Speed Factor](https://user-images.githubusercontent.com/9963310/134898389-4fde5090-c969-41b5-9c24-5a26914d0a55.png)

## Performing the Backlash Calibration

After having prepared everything, press the **Accept** button to perform the calibration. **NOTE:** the routine will test many aspects of the axis behavior, this will take many minutes. 

![Machine disable](https://user-images.githubusercontent.com/9963310/134893587-54c535b2-b019-4679-a8d7-ccc29ab505ed.png) Press the **Machine Disable** button, if you want to interrupt it. 

After the calibration has completed, go to Machine Setup / Axis to see the diagnostics.

## Backlash Calibration on the Axis / Diagnostics

The calibration function is also available on the Axis. 

![Calibrate button](https://user-images.githubusercontent.com/9963310/134895207-78b79c18-bf60-4bba-ba3e-1715d47a6547.png) Press the **Calibrate now** button to perform an axis backlash calibration at any time. 

The settings proposed by the routine can be edited as you like. Refer to the [[Backlash Compensation]] page for more information.

Elaborate graphical diagnostics are present after each calibration (both from Issues & Solutions and from the button). It contains a rich set of information about how the axis performs, that actually goes beyond the mere backlash behavior (explained below the screenshot): 

![Backlash Wizard](https://user-images.githubusercontent.com/9963310/134782398-0faaac87-1cbf-4c15-9f02-4bfb6ddb9ccb.png)

### Stepping Precision Graph

The first graph shows you the positional precision of the axis, stepping over one Millimeter. 

![Stepping Precision Graph](https://user-images.githubusercontent.com/9963310/134904889-dca2d798-7640-4325-a2a6-b766fe4447ee.png)

* The **Relative Step Errors** (blue dots) should remain inside the dark blue tolerance limits.  
* The **Absolute Error** (red line) should not show any sagging and no trend that goes outside the dark blue limits. 
* The **Random Move Error** (brown dots) should also mostly remain inside the dark blue tolerance limits. These dots represent random distance moves made with the newly calibrated backlash compensation settings applied, i.e. this is a success test. The same brown dots are also plotted in the second graph, this time over move distance. 

Failure to meet these goals may indicate that the motor currents are too weak and/or that the axis resolution/micro-stepping is too fine. The transmission ratio may be too large, e.g. the pulleys too large for the given motor power. Also check any dynamic motor current control such as Trinamic CoolStep™. You may have to switch it off.  

### Backlash Offset and Overshoot Graph

The second graph shows the measured **Backlash Offset** (blue) and **Overshoot** (red) at different approach distances. These are measured at minimum (usually 25%) speed and full speed, respectively. Both are performed after a straight and after a direction reversing move. The routine tries to find a minimum approach distance, where the backlash remains consistent within the given **Tolerance ±** window. This minimum approach distance with consistent backlash is called **Sneak-up Distance**. 

![Letter F Indicator](https://user-images.githubusercontent.com/9963310/134901088-453a60f2-1190-4a6c-a662-90149cafc2af.png)

#### Letter F Shaped Indicator

There is a dark blue letter-F-shaped indicator superimposed on the graph:

* The determined **Sneak-up Distance** is indicated by the vertical line of the letter "F".
* The determined consistent **Backlash Offsets** with **Tolerance ±** window is bracketed by the horizontal lines of the letter "F". 

#### Backlash Compensation Method Selection

* If the **Backlash Offset** and **Sneak-up Distance** are within **Tolerance ±**, the backlash compensation is switched off. Bravo for a superb machine!
* If the **Sneak-up Distance** is smaller than the average **Backlash Offset**, the most efficient **DirectionalCompensation** method is chosen. This method is as fast and fluid as with no compensation. 
* If the **Sneak-up Distance** is larger than the average **Backlash Offset** but still acceptable, the still quite efficient **DirectionalSneakUp** method is chosen.
* If the **Sneak-up Distance** is not acceptable, the classical **OneSidedPositioning** is chosen. This is a sign that backlash is large and rather inconsistent. Check the brown dots in the first graph for how consistently accurate the machine _really_ is (no guarantee at this point). 

### Move Time over Distance

The green line shows you the Move Time over distance. Note the [logarithmic scale](https://en.wikipedia.org/wiki/Logarithmic_scale), both on distance and move time. This is a good indicator of your axis performance. 

![Move Time](https://user-images.githubusercontent.com/9963310/134905005-c7e5b2df-d68c-49ac-aa18-356046128b48.png)

### Backlash at Sneak-up Speed

In this graph, the calibration routine measures **Backlash Offset** consistency at different approach speeds. If possible the speed is increased, up from the initial 25%. 

The graph also shows you how _effective_ the speed control is (green line). Ideally, the line is perfectly diagonal, i.e. effective speed factor and nominal speed factor (on the horizontal axis) are the same at each dot. 

![Speed Factor](https://user-images.githubusercontent.com/9963310/134905076-5ef2bb61-2ef5-4e92-b75b-11c5d0dc062a.png)

# Calibrating Precision Camera to Nozzle Offsets

To calibrate precision camera ↔ nozzle offsets, we let the nozzle pick, rotate and place a small test object and then measure the result using the camera. By exploiting the effects of rotational symmetry and averaging, the precise nozzle axis can be determined. 

The test object should be circular and flat on the top. The nozzle must be able to pick and place it precisely. There must be good contrast between test objects and background. As you are likely to lose or damage these little test objects, make many. They can be as simple as punched out "confetti" from a hole punch, used on matte card stock. Press them flat. Don't worry if they are not perfect, this will all be cancelled-out through symmetry. 

![Hole Punch Confetti](https://user-images.githubusercontent.com/9963310/119668622-a1aed380-be37-11eb-97cc-a99f7220ea04.jpg)
