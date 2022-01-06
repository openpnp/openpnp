## What is it?
Having a stable [[coordinate system|Machine-Axes#the-openpnp-coordinate-system]] early in the machine setup process is very important as all the locations you are capturing are referenced to it. Whenever the coordinate system changes/shifts, all these stored locations often become invalid and need to painstakingly be recaptured. 

Visual Homing allows you to reference your machine's X and Y axes to a [fiducial](https://en.wikipedia.org/wiki/Fiducial_marker#Printed_circuit_boards) on the machine table. This is often a more stable reference than axis end-switches, especially while you are still modifying and 
optimizing your machine. Axis limit switches trigger at varying places that can change with temperature and force applied when the head bangs into it. Thus they provide imprecise bounds on the movement range of the machine head.  [Liteplacer axis limit switch](https://www.liteplacer.com/gantry-back-plate-step-4-left-side-limit-switch/). Placing a fiducial permanently attached to the machine bed gives a precise location. The machine bounds together with a camera mounted on the head along with the machines movement range provides a means to precisely locate the fiducial because it’s view space exceeds the fuzzy imprecision of the limit switches. This combination of camera and limit switches can make a precise repeatable reference point on the machine. The precision offered by this approach is called visual homing and is a feature of many PnP machines. What follows is how OpenPnP supports visual homing and how you can implement it for your machine.

## Mounting a Fiducial

The standard fiducial is just any bright 1mm diameter round mark on dark ground. You can simply print the linked PDF. It is recommended to use a high quality matte photo paper or similar. Also make sure no scaling is in effect in the print settings: 

* [FiducialHome.pdf](https://github.com/openpnp/openpnp/files/5542424/FiducialHome.pdf)

Alternatively you can cut a fiducial out of a spare PCB. Other users have cleverly used the sprocket hole from a black plastic carrier tape on bright ground (1.5mm diameter).  

The fiducial needs to be mounted for "eternity". Choose a location where your machine is very unlikely to be modified in the future. A central location is ideal, as it halves any error in scale across the machine.

**IMPORTANT**: The fiducial must be mounted on the same Z level as the PCB surface. This makes sure any slight tilt in your camera is irrelevant. 

Ideally, every relevant location on your machine should be more or less flush with this same Z plane, e.g. feeders should be "sunken" so parts are picked near this Z coordinate. But don't bother if this principle is not perfectly adhered to. OpenPnP has a rich and growing system of calibrations that can compensate out errors.

## Creating the Fiducial Part

If you started from a standard OpenPnP installation, there is already a fiducial package `FIDUCIAL-1X2` defined. Otherwise, create it with these settings:

![Fiducial Package](https://user-images.githubusercontent.com/9963310/99186463-bdcb0d00-2750-11eb-8e11-4e465abc36e5.png)

Then check if there already is a part named `FIDUCIAL-HOME`. Otherwise, create one based on the package:

![FIDUCIAL-HOME](https://user-images.githubusercontent.com/9963310/99186516-084c8980-2751-11eb-9685-b07768e979d2.png)

## Setting up Visual Homing

For the final step, go to the machine Head and set up Visual Homing. There are two methods implemented: 

* A new method, recommended for new machines. 

* A legacy method that aims to keep compatibility for existing machines **to preserve their coordinate system**.

___
**CAUTION**: Do not be tempted to just switch to the new method on an old machine unless you are absolutely sure to start from scratch with everything! There is a guide to to migrate the new method [below](#migrate-to-new-resettofiduciallocation-method). 
___

### Method for new Machines

**Basic Operation Theory**: Before visual homing is performed, the controller is already mechanically homed by end-switches on the axes (some users also position the head manually). The coordinate system must now already _roughly_ be right, the Visual Homing is only used to nail the coordinate system down more precisely. 

![Visual Homing](https://user-images.githubusercontent.com/9963310/99185536-c02a6880-274a-11eb-8416-ecf0c2447a66.png)

**Homing Fiducial** locates the fiducial on the machine in OpenPnP coordinates. You can simply jog to the fiducial and capture it using the Capture button:

![Capture Fiducial](https://user-images.githubusercontent.com/9963310/99187007-62028300-2754-11eb-91cd-10921e3b6f74.png)

You can also manually set the fiducial coordinates to more "round" numbers that corresponds roughly to the fiducial location. It has to be within a few millimeters of the mechanically homed (captured) coordinates. 

With the new method you can rest assured that axis transformations such as [[Non-Squareness Compensation|Linear-Transformed-Axes#use-case--non-squareness-compensation]] are properly handled back and forth. However, naturally, the non-squareness compensation must be configured **before** the homing fiducial is captured. 

**Homing Method** determines how the coordinate system is reset, relative to the fiducial.  

![Homing Method](https://user-images.githubusercontent.com/9963310/99186600-be17d800-2751-11eb-801d-ba64f021625f.png)

* **None**: Switches off visual homing.

* **ResetToFiducialLocation**: use this method for a newly set up machine. After the fiducial has been pinned down by Computer Vision, the machine position is reset to the **Homing Fiducial** location i.e. the theoretical (rounded) fiducial location and the actual machine location are made to match. 

* **ResetToHomeLocation**: do **not** use this method for a newly set up machine. 

### Method for old Machines

**Basic Operation Theory**: Before visual homing is performed, the controller is already mechanically homed by end-switches on the axes (some users also position the head manually). The coordinate system can be anything. Visual Homing will completely redefine the coordinate system to the **Home Coordinate** set on the X and Y axes. Axis transformations are not accounted for. 

**NOTE**: you should never change these setting for an existing machine. The following is just intended to _understand_ what has been migrated automatically from previous versions of OpenPnP 2.0. There is a guide to convert to the new method [below](#migrate-to-new-resettofiduciallocation-method).

![Visual Homing OLD](https://user-images.githubusercontent.com/9963310/99187254-bd814080-2755-11eb-85e9-f3d9ef0efc3c.png)

**Homing Fiducial** locates the fiducial on the machine. The coordinate cannot be captured safely because the coordinate system after mechanical homing can be completely different. As this is for existing machines, it is not elaborated further. Use the new method if you really need to redefine the fiducial location. 

**Homing Method** determines how the coordinate system is reset, relative to the fiducial.  

![Homing Method OLD](https://user-images.githubusercontent.com/9963310/99187461-0ab1e200-2757-11eb-93c3-8503e058fa22.png)

* **None**: Switches off visual homing.

* **ResetToFiducialLocation**: do **not** use this method for an already set up machine that has **ResetToHomeLocation**! 

* **ResetToHomeLocation**: after mechanical homing, OpenPnP will move the camera to the Fiducial Location using the mechanical coordinate system. Computer Vision will then pin the fiducial down precisely. Afterwards this location is reset to the X and Y axis **Home Coordinates**. See the [[Machine Axis Controller Settings for how to set the Homing Coordinate|Machine-Axes#controller-settings]]. These are raw coordinates so axis transformations such as [[Non-Squareness Compensation|Linear-Transformed-Axes#use-case--non-squareness-compensation]] will not be applied, the coordinates may therefore appear differently in OpenPnP's DRO. 

### Migrate to new **ResetToFiducialLocation** Method

This is a procedure to "migrate" to the new **ResetToFiducialLocation** method, without losing any coordinates you already captured. It is a delicate, manual procedure, that unfortnuately cannot be automated in the general case. 

1. This works easily, if your fiducial was very near the electro-mechanical homing coordinates. Otherwise it will get more complicated, see the last few points.
1. This is delicate: follow this carefully!
1. Leave your existing **old** homing fiducial physically intact **(important!)**.
1. Mount a **new** fiducial in a more central location, both in X and Y (as described [above](#mounting-a-fiducial)).
1. Start a fresh OpenPnP session.
1. Perform a full machine homing. Make sure the visual homing was successful and your old homing fiducial is perfectly in the cross-hairs, when you move to the home location coordinates.
1. While the machine is still homed that way, do the following steps. **Don't interrupt this!**
1. Set **Homing Method** on the head to **ResetToFiducialLocation**, press **Apply**.
1. Jog to the **new** homing fiducial, press **Visual Test** to center the camera perfectly.
1. Capture the new **Homing Fiducial** location using the usual blue camera button. Press **Apply**.
1. Test a full machine homing. It should now use the **new** fiducial and the modern **ResetToFiducialLocation** method. If this works out, you're already done.
1. If it does not find the **new** fiducial, then your former **ResetToHomingLocation** configuration had a fiducial location that did not match the homing location at all, perhaps due to a home-to-max config, large retract, or something. The electro-mechanically homed coordinate system is too different from the visually homed one. You need to figure out the **shift in X/Y**.
1. Alternative A: if you have the TinyG, set the home coordinates on the X, Y Axes in OpenPnP to correspond to the visually homed coordinate system (compensate for the shift). Then delete (empty) your existing `HOME_COMMAND` and let [[Issues and Solutions]] generate a dynamic one.
1. Alternative B: in our controller's config (e.g. Smoothieware `config.txt`), set the X, Y home coordinates to correspond to the visually homed coordinate system (compensate for the shift).
1. Alternative C: Add G-code to your `HOME_COMMAND` to roughly move the camera over the old homing fiducial using coordinates of the electro-mechanically homed machine coordinate system and then reset this position to the visually homed coordinate system, something like this:

    ```
    ... ; existing homing sequence
    G1 X10 Y650 ; move roughly over the old fiducial (but use your raw old fiducial coordinates!)
    G92 X0 Y0 ; reset to old home coordinates (but use your old home coordinates!)
    ```

1. Retry from step 11.

___

## Advanced Motion Control Topics

### Motion Control
- [[Advanced Motion Control]]
- [[GcodeAsyncDriver]]
- [[Motion Planner]]
- [[Visual Homing]]
- [[Motion Controller Firmwares]]

### Machine Axes
- [[Machine Axes]]
- [[Backlash-Compensation]]
- [[Transformed Axes]]
- [[Linear Transformed Axes]]
- [[Mapping Axes]] 
- [[Axis Interlock Actuator]]

### General
- [[Issues and Solutions]]
