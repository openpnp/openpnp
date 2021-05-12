# What is it?

Visual calibration uses a holistic (whole-machine) approach to calibrate both the cameras and various mechanics of the machine by playing the two against each other. On one hand, the known metrics of machine motion are used to calibrate the cameras. On the other hand, the camera is used to make the motion more precise. The inherent chicken and egg situation between the two is circumvented by exploiting various forms of symmetry and by approximation through iteration, things that would be hard or tedious to do by hand. 

# Calibration Rig

In order to perform Visual Calibration, a certain calibration rig is needed. The elements of which is described here. 

## Calibration Base Fiducial

Camera calibration can be performed automatically by looking at a fiducial while moving the camera around in a certain pattern. 

The fiducial must be round, high contrast, sharp and flat. You can print one on matte paper using the best quality print settings.

Just paste this ● (Unicode character 9679 / 0x25CF) into an editor and print at 10pt.

The camera must be free to move around the fiducial so that it can appear at the edge of the camera view all around i.e. do not place the fiducial at the very edge of the machine motion range. 

As the camera and the light will move around and point at the fiducial from various angles, the fiducial must not have any depth or 3D structure, and it must not be shiny, i.e. lighting it from the side should be fine. Don't use through holes, carrier tape sprocket holes, prints on glossy photo paper, HASL PCB fiducials or similar.

The fiducial must be mounted precisely on PCB surface Z Level, one way to make sure is to glue the printed-out fiducial flat onto a surplus PCB that you can position reproducibly (±1 mm) on its holder.

