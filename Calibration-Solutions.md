# What is it?

Calibration Solutions are provided by [[Issues and Solutions]] when targeting the **Calibration** milestone. This page as a whole provides the background information for it. Individual sections are linked from the corresponding solutions.  

The Calibration Solutions are part of an overall holistic (whole-machine) approach to calibrate both the cameras and various mechanics of the machine by playing the two against each other. On one hand, the known metrics of machine motion are used to calibrate the cameras. On the other hand, the camera is used to make the motion more precise. The inherent chicken and egg situation between the two is circumvented by exploiting various forms of symmetry and by approximation through iteration, things that would be hard or tedious to do by hand. 

# Calibrating Backlash Compensation

Backlash compensation is used to avoid the effects of any looseness or play in the mechanical linkages of machine axes. More information can be found on the [[Backlash Compensation]] page. 

Using the camera and looking at the calibration primary fiducial, the calibration process can automatically determine the backlash of the machine in X and Y. It can therefore configure [[Backlash Compensation]] with the correct offsets to perfectly compensate. The **DirectionalCompensation** is used for best performance. 

