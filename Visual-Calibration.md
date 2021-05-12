# What is it?

Visual calibration uses a holistic (whole-machine) approach to calibrate both the cameras and various mechanics of the machine by playing the two against each other. On one hand, the known metrics of machine motion are used to calibrate the cameras. On the other hand, the camera is used to make the motion more precise. The inherent chicken and egg situation between the two is circumvented by exploiting various forms of symmetry and by approximation through iteration, things that would be hard or tedious to do by hand. 

Visual Calibration is provided by [[Issues and Solutions]] when targeting the **Calibration** milestone. This page as a whole provides the background information for it. Individual sections are linked from the corresponding solutions.  

# Calibration Rig

In order to perform Visual Calibration, a certain calibration rig is needed. The elements of which is described here. 

## Calibration Base Fiducial

Camera calibration can be performed automatically by looking at a [fiducial](https://en.wikipedia.org/wiki/Fiducial_marker#Printed_circuit_boards) while moving the camera around in a certain pattern. 

The fiducial must be round, high contrast, sharp and flat. The standard fiducial (also used for [[Visual Homing]]) is just any bright 1mm diameter round mark on dark ground. You can simply print the linked PDF. It is recommended to use a high quality matte photo paper or similar: 

* [FiducialHome.pdf](https://github.com/openpnp/openpnp/files/5542424/FiducialHome.pdf)

The camera must be free to move around the fiducial so that it can appear at the edge of the camera view all around i.e. do not place the fiducial at the very edge of the machine motion range. 

As both the camera and the camera light will move around and point at the fiducial from various angles, the fiducial must not have any depth or 3D structure, and it should have highly [diffuse reflection](https://en.wikipedia.org/wiki/Diffuse_reflection) so lighting it from the side should be fine. Don't use through holes, carrier tape sprocket holes, prints on glossy photo paper, HASL PCB fiducials or similar.

The fiducial must be mounted precisely on PCB surface Z Level, one way to make sure, is to glue the printed-out fiducial flat onto a surplus PCB that you can position reproducibly (±1 mm) on its holder.

