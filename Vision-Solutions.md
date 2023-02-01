# What is it?

Vision Solutions are provided by [[Issues and Solutions]] when targeting the **Vision** milestone. This page as a whole provides the background information for it. Individual sections are linked from the corresponding solutions.  

The Vision Solutions are part of an overall holistic (whole-machine) approach to calibrate both the cameras and various mechanics of the machine by playing the two against each other. On one hand, the known metrics of machine motion are used to calibrate the cameras. On the other hand, the camera is used to make the motion more precise. The inherent chicken and egg situation between the two is circumvented by exploiting various forms of symmetry and by approximation through iteration, things that would be hard or tedious to do by hand. 

To get some impressions, [watch a video of these and other calibration solutions](https://youtu.be/md68n_J7uto). The video is not complete, so come back to this page.

# Calibration Rig

In order to perform Visual Calibration, a certain calibration rig is needed. The elements are described in the respective calibration steps. 

![Calibration rig](https://user-images.githubusercontent.com/9963310/129459527-52675532-f63c-458e-8dec-73659cffe3ab.png)

# Calibration Steps

## Calibration Primary Fiducial

Preliminary camera calibration can be performed automatically by looking at a [fiducial](https://en.wikipedia.org/wiki/Fiducial_marker#Printed_circuit_boards) while moving the camera around in a certain pattern. 

The fiducial must be round, ideally it is high contrast, sharp and flat, i.e., with no sunken/raised "3D" structure. It must appear as a single unambiguous circular feature, even when seen from the side. Other than these requirements, the fiducial can be anything. The recommended fiducial is just any bright 1mm diameter round mark on dark ground  (the same type is used for [[Visual Homing]]). You can simply print the linked PDF, it is recommended to use a high-quality matte photo paper or similar: 

* [FiducialHome.pdf](https://github.com/openpnp/openpnp/files/5542424/FiducialHome.pdf)

The camera must be free to move in a wide area (250mm × 250mm) centered around the fiducial so that the camera can perform some fast moves for various calibration purposes. Do not place the fiducial at the edge of the machine motion range. 

As both the camera and the camera light will move around and point at the fiducial from various angles, the fiducial must not have any depth or 3D structure, and it should have [diffuse reflection](https://en.wikipedia.org/wiki/Diffuse_reflection) so lighting it from the side should be fine. Don't use through holes, carrier tape sprocket holes, prints on glossy photo paper, HASL PCB fiducials or similar.

The fiducial must be mounted precisely on PCB surface Z Level, one way to make sure of this, is to glue the printed-out fiducial flat onto a surplus PCB that you can position reproducibly (±1 mm) on its holder. When capturing the location of the fiducial, the Z coordinate is the most critical as any error there will result is a camera scaling (units per pixel) error.

On the solution, you can adjust the **Feature Diameter**. 

Press the **Auto-Adjust** button. A range of diameters will be scanned, the one with the best score will be selected: 

![auto_feature_diameter](https://user-images.githubusercontent.com/9963310/130499531-925a64db-d948-4fdf-a664-c13054c799df.gif)

Note, this will likely work reliably for fiducials, but for nozzle tips, manual adjustment might still be needed, to get the right feature, such as the air bore. See [Up-looking Camera Offsets](#up-looking-camera-offsets). 

For manual settings, use the spin control. On new machines/cameras, nothing is yet known about the camera viewing scale, therefore the adjustment is in pixels rather than real length units. Adjust the diameter up/down until a green circle and crosshairs appear. The circle should hug the fiducial contour, just slightly outside. It will snap to it across a certain range of given diameters, it is sufficient to be in that range. Remember, you can zoom the camera view using the mouse scroll-wheel. 

## Calibration Secondary Fiducial

Preliminary camera calibration also requires looking at a secondary fiducial at different height (Z level). This will provide the calibration algorithm with the needed 3D/spatial information to determine the true focal length of the lens and the optical position of the camera in space. 

The secondary fiducial should be placed close to the primary fiducial (see previous section), but at a different Z level (be sure to keep it below the safe Z level). The fiducial may be out of focus, but the blur should not go all the way across. In the illustration below the middle one is still good, the right one not. As with the primary fiducial, when capturing the location of the secondary fiducial, the Z coordinate is the most critical as any error there will result is a camera scaling (units per pixel) error. 

![Out of Focus](https://user-images.githubusercontent.com/9963310/118112263-0e13e680-b3e5-11eb-818b-64d157866b35.png)

The placement must not obstruct the camera view on either fiducial from all around them, again so that they can appear anywhere inside and to the edge of the camera view. 

You can print out the same fiducial as for the primary fiducial, cut it out with a margin of a few millimeters, and glue it onto a spacer. The spacer should be as black and matte as possible. All the other rules for the primary fiducial apply equally.

## Nozzle Offsets

After the calibration fiducial X, Y coordinates have been determined, we also need the Z coordinates. For this we need the nozzle tip (the very point) as the reference. Therefore, you need to load a nozzle tip to each nozzle and then perform the nozzle offset calibration solutions. 

In the spirit of the holistic approach described above, this step goes both ways: The fiducial will get its Z coordinate, and the nozzle will get its approximate head offsets in X, Y (these will be calibrated to better precision later). 

For multi-nozzle machines, the first nozzle (default nozzle) will serve as the head Z reference. All the other nozzles will be adjusted to the same Z reference. 

As a side effect, the fiducial Z coordinates (primary and secondary together) will also complete the 3D Units per Pixel calibration. See the [[3D Units per Pixel]] page for more information.

## Down-looking Camera Offsets

For machines with multiple down-looking camera on the same head, the additional cameras equally need to calibrate head offsets. Move the camera to the primary calibration fiducial and accept the solution. 

## Up-looking Camera Offsets

Up-looking cameras (also known as "bottom cameras") cannot look at the calibration fiducial, obviously, so they are calibrated against a nozzle tip. Load the smallest nozzle tip that you can reliably detect (it may take some trial and error). 

![bottom camera calibration](https://user-images.githubusercontent.com/9963310/130501778-de6cd944-cbae-40d3-87a4-bd00dbfc6c15.png)

