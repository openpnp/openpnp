## What is it?
Having a stable [[coordinate system|Machine-Axes#the-openpnp-coordinate-system]] early in the machine setup process is very important as all the locations you are capturing are referenced to it. Whenever the coordinate system changes/shifts, all these stored locations often become invalid and need to painstakingly be recaptured. 

Visual Homing allows you to reference your machine's X and Y axes to a [fiducial](https://en.wikipedia.org/wiki/Fiducial_marker#Printed_circuit_boards) on the machine table. This is often a more stable reference than axis end-switches, especially while you are still modifying and optimizing your machine or when the end-switch mounting is very "DIY" such as in [Liteplacer's "one screw is enough" way](https://www.liteplacer.com/gantry-back-plate-step-4-left-side-limit-switch/).  

## Mounting a Fiducial

The standard fiducial is just any bright 1mm diameter round mark on dark ground. You can simply print the linked PDF. It is recommended to use a high quality matte photo paper or similar. Also make sure no scaling is in effect in the print settings: 

* [FiducialHome.pdf](https://github.com/openpnp/openpnp/files/5542424/FiducialHome.pdf)

Alternatively you can cut a fiducial out of a spare PCB. Other users have cleverly used the sprocket hole from a black plastic carrier tape on bright ground (1.5mm diameter).  

The fiducial needs to be mounted for "eternity". Choose a location where your machine is very unlikely to be modified in the future. A central location is ideal, as it halves any error in scale across the machine.

## Creating the Fiducial Part

If you started from a standard OpenPnP installation, there is already a fiducial package `FIDUCIAL-1X2` defined. Otherwise, create it with these settings:

![Fiducial Package](https://user-images.githubusercontent.com/9963310/99186463-bdcb0d00-2750-11eb-8e11-4e465abc36e5.png)

Then check if there already is a part named `FIDUCIAL-HOME`. Otherwise, create one based on the package:

![FIDUCIAL-HOME](https://user-images.githubusercontent.com/9963310/99186516-084c8980-2751-11eb-9685-b07768e979d2.png)

## Setting up Visual Homing

For the final step, go to the machine Head and set up Visual Homing. There are two methods implemented: 

* A legacy method that aims to keep compatibility for existing machines **to preserve their coordinate system**.

* A new method, recommended for new machines. 

___
**CAUTION**: Do not be tempted to use the new method on an old machine unless you are absolutely sure to start from scratch with everything!
___

### Method for new Machines

**Basic Operation Theory**: Before visual homing is performed, the controller is already mechanically homed by end-switches on the axes (some users also position the head manually). The coordinate system must now already _roughly_ be right, the Visual Homing is only used to nail the coordinate system down more precisely. 

![Visual Homing](https://user-images.githubusercontent.com/9963310/99185536-c02a6880-274a-11eb-8416-ecf0c2447a66.png)

**Homing Fiducial** locates the fiducial on the machine in OpenPnP coordinates. You can simply jog to the fiducial and capture it using the Capture button:

![Capture Fiducial](https://user-images.githubusercontent.com/9963310/99187007-62028300-2754-11eb-91cd-10921e3b6f74.png)

You can also manually set the fiducial coordinates to more "round" numbers that corresponds roughly to the fiducial location. It has to be within a few millimeters of the mechanically homed (captured) coordinates. 

With the new method you can rest assured that axis transformations such as [[Non-Squareness Compensation|Linear-Transformed-Axes#use-case--non-squareness-compensation]] are properly handled back and forth. 

**Homing Method** determines how the coordinate system is reset, relative to the fiducial.  

![Homing Method](https://user-images.githubusercontent.com/9963310/99186600-be17d800-2751-11eb-801d-ba64f021625f.png)

* **None**: Switches off visual homing.

* **ResetToFiducialLocation**: use this method for a newly set up machine. After the fiducial has been pinned down by Computer Vision, the machine position is reset to the **Homing Fiducial** location i.e. the theoretical (rounded) fiducial location and the actual machine location are made to match. 

* **ResetToHomeLocation**: do **not** use this method for a newly set up machine. 

### Method for old Machines

**Basic Operation Theory**: Before visual homing is performed, the controller is already mechanically homed by end-switches on the axes (some users also position the head manually). The coordinate system can be anything. Visual Homing will completely redefine the coordinate system to the **Home Coordinate** set on the X and Y axes. Axis transformations are not accounted for. 

**NOTE**: you should never change these setting for an existing machine. The following is just intended to understand what has been migrated automatically from previous versions of OpenPnP 2.0. 

![Visual Homing OLD](https://user-images.githubusercontent.com/9963310/99187254-bd814080-2755-11eb-85e9-f3d9ef0efc3c.png)

**Homing Fiducial** locates the fiducial on the machine. The coordinate cannot be captured safely because the coordinate system after mechanical homing can be completely different. As this is for existing machines, it is not elaborated further. Use the new method if you really need to redefine the fiducial location. 

**Homing Method** determines how the coordinate system is reset, relative to the fiducial.  

![Homing Method OLD](https://user-images.githubusercontent.com/9963310/99187461-0ab1e200-2757-11eb-93c3-8503e058fa22.png)

* **None**: Switches off visual homing.

* **ResetToFiducialLocation**: do **not** use this method for an already set up machine! 

* **ResetToHomeLocation**: after mechanical homing, OpenPnP will move the camera to the Fiducial Location using the mechanical coordinate system. Computer Vision will then pin the fiducial down precisely. Afterwards this location is reset to the X and Y axis **Home Coordinates**. See the [[Machine Axis Controller Settings for how to set the Homing Coordinate|Machine-Axes#controller-settings]]. These are raw coordinates so axis transformations such as [[Non-Squareness Compensation|Linear-Transformed-Axes#use-case--non-squareness-compensation]] will not be applied, the coordinates may therefore appear differently in OpenPnP's DRO. 

