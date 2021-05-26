# What is it?

Calibration Solutions are provided by [[Issues and Solutions]] when targeting the **Calibration** milestone. This page as a whole provides the background information for it. Individual sections are linked from the corresponding solutions.  

The Calibration Solutions are part of an overall holistic (whole-machine) approach to calibrate both the cameras and various mechanics of the machine by playing the two against each other. On one hand, the known metrics of machine motion are used to calibrate the cameras. On the other hand, the camera is used to make the motion more precise. The inherent chicken and egg situation between the two is circumvented by exploiting various forms of symmetry and by approximation through iteration, things that would be hard or tedious to do by hand. 

# Calibrating Backlash Compensation

Backlash compensation is used to avoid the effects of any looseness or play in the mechanical linkages of machine axes. More information can be found on the [[Backlash Compensation]] page. 

Using the camera and looking at the calibration primary fiducial, the calibration process can automatically determine the backlash of the machine in X and Y. It can therefore configure [[Backlash Compensation]] with the correct offsets to perfectly compensate. The **DirectionalCompensation** is used for best performance. 

# Calibrating Precision Camera to Nozzle Offsets

To calibrate precision camera ↔ nozzle offsets, we let the nozzle pick, rotate and place a small test object and then measure the result using the camera. By exploiting the effects of rotational symmetry and averaging, the precise nozzle axis can be determined. 

The test object should be circular and flat on the top. The nozzle must be able to pick and place it precisely. There must be good contrast between test objects and background. As you are likely to lose or damage these little test objects, make many. They can be as simple as punched out "confetti" from a hole punch, used on matte card stock. Press them flat. Don't worry if they are not perfect, this will all be cancelled-out through symmetry. 

![Hole Punch Confetti](https://user-images.githubusercontent.com/9963310/119668622-a1aed380-be37-11eb-97cc-a99f7220ea04.jpg)
