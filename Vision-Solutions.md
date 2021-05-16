# What is it?

Vision Solutions are provided by [[Issues and Solutions]] when targeting the **Vision** milestone. This page as a whole provides the background information for it. Individual sections are linked from the corresponding solutions.  

The Vision Solutions are part of an overall holistic (whole-machine) approach to calibrate both the cameras and various mechanics of the machine by playing the two against each other. On one hand, the known metrics of machine motion are used to calibrate the cameras. On the other hand, the camera is used to make the motion more precise. The inherent chicken and egg situation between the two is circumvented by exploiting various forms of symmetry and by approximation through iteration, things that would be hard or tedious to do by hand. 

# Calibration Rig

In order to perform Visual Calibration, a certain calibration rig is needed. The elements of which is described here. 

## Calibration Primary Fiducial

Camera calibration can be performed automatically by looking at a [fiducial](https://en.wikipedia.org/wiki/Fiducial_marker#Printed_circuit_boards) while moving the camera around in a certain pattern. 

The fiducial must be round, high contrast, sharp and flat. The standard fiducial (the same type is used for [[Visual Homing]]) is just any bright 1mm diameter round mark on dark ground. You can simply print the linked PDF. It is recommended to use a high quality matte photo paper or similar: 

* [FiducialHome.pdf](https://github.com/openpnp/openpnp/files/5542424/FiducialHome.pdf)

The camera must be free to move around the fiducial so that it can appear at the edge of the camera view all around i.e. do not place the fiducial at the very edge of the machine motion range. 

As both the camera and the camera light will move around and point at the fiducial from various angles, the fiducial must not have any depth or 3D structure, and it should have [diffuse reflection](https://en.wikipedia.org/wiki/Diffuse_reflection) so lighting it from the side should be fine. Don't use through holes, carrier tape sprocket holes, prints on glossy photo paper, HASL PCB fiducials or similar.

The fiducial must be mounted precisely on PCB surface Z Level, one way to make sure of this, is to glue the printed-out fiducial flat onto a surplus PCB that you can position reproducibly (±1 mm) on its holder.

## Calibration Secondary Fiducial

Camera calibration also requires looking at a secondary fiducial at different height (Z level). This will provide the calibration algorithm with the needed 3D/spacial information to determine the true focal length of the lens and the optical position of the camera in space. 

The secondary fiducial should be placed close to the primary fiducial (see previous section), but on an elevated (or lowered) level in Z. The fiducial may be out of focus, but the blur should not go all the way across. In the illustration below the middle one is still good, the right one not.

![Out of Focus](https://user-images.githubusercontent.com/9963310/118112263-0e13e680-b3e5-11eb-818b-64d157866b35.png)

The placement must not obstruct the camera view on either fiducials from all around them, again so that they can appear anywhere inside and to the edge of the camera view. 

You can print out the same fiducial as for the primary fiducial, cut it out with a margin of a few millimeters, and glue it onto a spacer. The spacer should be as black and matte as possible. All the other rules for the primary fiducial apply equally.

## Nozzle Offsets

After the calibration fiducial X, Y coordinates have been determined, we also need the Z coordinates. For this we need the nozzle tip (the very point) as the reference. Therefore, you need to load a nozzle tip to each nozzle and then perform the nozzle offset calibration solutions. 

In the spirit of the holistic approach described above, this step goes both ways: The fiducial will get its Z coordinate, and the nozzle will get its approximate head offsets in X, Y (these will be calibrated to better precision later). 

For multi-nozzle machines, the first nozzle (default nozzle) will serve as the head Z reference. All the other nozzles will be adjusted to the same reference. 
