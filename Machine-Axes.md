## The OpenPnP Coordinate System
OpenPnP conceptually uses a [Cartesian Coordinate System](https://en.wikipedia.org/wiki/Cartesian_coordinate_system). Assuming you stand in front of the machine, the X axis points to the right, Y away from you and Z up. The rotational axis C rotates around Z, counter-clockwise.

![Axis Coordinate System](https://user-images.githubusercontent.com/9963310/95686141-663efc00-0bfc-11eb-8a7a-153ab472f871.png)

The coordinate system now needs to be defined in OpenPnP. 

In the Machine Setup tab's hierarchical view, expand the Axes branch. Most likely you will already see defined axes, either migrated from an earlier version of OpenPnP or the default set. 

Click on one of the existing axes or create a new one using the 
![Plus](https://user-images.githubusercontent.com/9963310/95689795-9f369b00-0c13-11eb-8347-7d4645776a0f.png) button and selecting the class:

![Select Axis Type](https://user-images.githubusercontent.com/9963310/95689720-0f90ec80-0c13-11eb-9c9d-aa33fd7888cf.png)

## ReferenceControllerAxis

The ReferenceControllerAxis is the primary controller-attached axis of a machine.

![Linear Axis Setup](https://user-images.githubusercontent.com/9963310/95686619-452bda80-0bff-11eb-89e2-26283b7fa8d9.png) 

### Properties

**Type** defines the axis meaning inside the Cartesian Coordinate System. OpenPnP can have multiple axes of the same Type, typically multiple Z and multiple C axes for multi-nozzle machines. 

![Axis Type](https://user-images.githubusercontent.com/9963310/95687314-10ba1d80-0c03-11eb-99d9-a638aecbf31b.png)

**Name** is your conceptual axis name. 

### Controller Settings

**Driver** links the axis to the driver which talks to the controller that controls its motion. 

**Axis Letter** gives you the letter of the axis by which it is addressed in the controller (usually a G-code letter). The letter must obviously be unique for a given driver. If you have multiple drivers, you typically have the same letter multiple times (and that is why inside OpenPnP you refer to the axis by its conceptual **Name** rather than its **Letter**). 

**Switch Linear ↔ Rotational**: G-code controllers make a difference between linear and rotational axes. The difference is relevant for the correct interpretation of feed-rates, acceleration limits and other issues. If you have a machine with more than one controller and/or more than two nozzles, chances are high that you are forced to be using a linear controller axis (typically X Y Z) to move what is conceptually a rotational axis (A B C) or vice versa. 
OpenPnP needs to know if this is the case, so use the switch to toggle the axis meaning. 

**Home Coordinate** gives OpenPnP the initial coordinate after homing. This may include a retract distance. 

**Backlash Compensation** will be treated later, on the [[Backlash-Compensation]] page. For the first basic setup leave it at **None**. 

**Resolution [Driver Units]** indicates the smallest step that OpenPnP should consider as a change in coordinate, also sometimes named a resolution "tick". It should be equal to a micro-step (or a practical multiple thereof). The larger the Resolution tick is, the more often OpenPnP can optimize axis moves away (especially in the small adjustments of non.squareness compensation, nozzle tip runout compensation etc.). By preventing unneeded micro axis move, OpenPnP can also prevent extra backlash compensation moves (not relevant with DirectionalCompensation). 

### Kinematic Settings / Axis Limits

**Soft Limit Low** and **Soft Limit High** bracket the valid range of the axis. OpenPnP will refuse a move to a Location if it leads outside this range. 

![Soft Limit](https://user-images.githubusercontent.com/9963310/95889274-7e408800-0d82-11eb-9b45-d275c7d96874.png)

The respective Limit is only enforced if **Enabled?** is selected. 

![Capture Axis Limit](https://user-images.githubusercontent.com/9963310/95889746-1c345280-0d83-11eb-8386-07d856e85958.png) Use the **Capture** button to capture the current axis position as the new limit.

![Position to Axis Limit](https://user-images.githubusercontent.com/9963310/95889714-0e7ecd00-0d83-11eb-8945-e49f43e80d5b.png) Use the **Position** button to move the axis to its current limit. 

Within the soft limits, you can define the **Safe Zone Low** and **Safe Zone High** limits. This is mandatory for the Z axis, to define the so-called **Safe Z**. To avoid collisions with anything on the machine table, Nozzles (and other so-called Head-Mountables) are lifted to **Safe Z** before the Head is allowed to move in X and Y (or C). 

The **Safe Zone** can be the same value for **Low** and **High**, defining a single **Safe Z** height. Or it can be a range, to give OpenPnP more freedom to optimize its operation. 

Multi-nozzle machines often share one physical Z axis to make two Nozzles move in a counterweight, seesaw or rocker configuration ([more on that later](#)). If you want your **Safe Z** at the perfect balance-point of the two Nozzles, choose the mid-point of the Z axis as the **Safe Z** height, i.e. **Low/High** at the same value. You should also use this setting as a starting point, if you are still in the process of defining the axes for the first time (chicken egg problem). 

If your machine is already setup, you can revisit the Z axis and optimize. The Nozzles may have a lot of Z headroom, i.e. it is quite slow to move them all the way to the balance-point. So you can choose individual **Safe Z** heights for the Nozzles and set them as the **Safe Zone Low** and **High** limits respectively (you will see which is which when you Jog them up and down). OpenPnP is then free to optimize and only lift the nozzle up as far as needed. But be prepared for the laid-back/untidy "hanging nozzle style" ;-) 

If there is a switch of nozzles (one going up, move in X/Y, the _other_ going down), OpenPnP will even exploit the time in transit to move the Z axis over to the _other_ limit of the Safe Zone, readying the _other_ Nozzle at the exit point for the quickest/shortest descent to the target location.  

The other linear axes (X, Y) may also have a **Safe Zone**. If you combine the Safe Zones of all the axes, you get a box in space that is considered safe for any motion (no collisions). Inside that box, OpenPnP is free to do any motion. This is exploited for [[Advanced-Motion-Control]].

### Kinematic Settings / Rate Limits

**Feedrate [/s]** sets the speed limit on the axis. **NOTE**: Unlike in G-code `F` words, this limit is per second rather than per minute. It seemed more consistent to define all rates (Feedrate/Acceleration/Jerk) in the same [s] time unit. You need to divide by 60 if converting from G-code feed-rates. 

The Axes can have different speed limits which is especially important for the rotational axes where the limit is angular [°/s] rather than linear [mm/s]. In typical DIY stepper machines the angular speed can be much higher than the linear speed, e.g. a 180° nozzle turn should be quicker than a 180mm linear move. 

When OpenPnP is performing a move with several axes involved (e.g. a diagonal plus rotation), it will exploit the speed limit on all the axes. I.e. the overall speed along the diagonal can be significantly faster than for an individual axis (this behavior is optional, see [[Advanced-Motion-Control]]). 

**Acceleration [/s²]** sets the [acceleration](https://simple.wikipedia.org/wiki/Acceleration) limit (maximum change of velocity per unit of time). In many cases this is more important than the speed limit, as the speed limit is only ever reached in very long moves. 

**Jerk [/s³]** sets the [jerk](https://simple.wikipedia.org/wiki/Jerk) limit (maximum change of acceleration per unit of time). If left at zero, no jerk control will be used. Without jerk control, the acceleration will be switched on and off instantaneous, creating vibrations and wear and tear. OpenPnP has several options to use jerk control, even if your controller does not natively have the capability (see [[Advanced-Motion-Control]]).

## ReferenceVirtualAxis

The ReferenceVirtualAxis is a virtual stand-in for a real machine axis. There is typically a virtual Z and C assigned to the down-looking Camera. 

![Virtual Axis](https://user-images.githubusercontent.com/9963310/95973525-1175ce00-0e14-11eb-9c3b-c37ade63eadd.png)

### Virtual Axis / Settings

Aside from the basic Type and Name (explained for the [ReferenceControllerAxis](#reference-controller-axis)), only the **Home Coordinate** needs to be set as the initial position of the axis. This also doubles as Safe Z for this axis, i.e. if you press the Z axis **P** button in the Machine Controls it goes to this coordinate. 

### Use Case / Example
Its purpose is to store or prepare a coordinate for Z or C while working with the camera as the selected tool in the Machine Controls. 

![Virtual Machine Controls](https://user-images.githubusercontent.com/9963310/95972631-fa82ac00-0e12-11eb-8cbd-7df0018b6677.png) 

Assume you have moved your nozzle to the pick location of a feeder. The nozzle tip is right over the part. The Z axis of the nozzle now gives you the Z of the feeder. 

But it's hard to judge the X/Y precisely from the side, and you have no idea of the rotation (C) of the part.

![Move Camera to Nozzle](https://user-images.githubusercontent.com/9963310/95973733-513cb580-0e14-11eb-8233-5e660b863365.png) Use the **Move Camera to Nozzle** button to move the camera over the part. Doing so will go to Safe Z first, so the Z you carefully adjusted would be lost, if it weren't for the **Z virtual axis** of the camera that is then set to the former Z coordinate to safeguard. 

In the Machine Controls, select the camera. Then make sure to have the pick location of the feeder in the crosshairs of the camera, so the X and Y are also precisely set. There is no real/physical C axis on a camera but it does still make sense to use the C machine controls to rotate the crosshairs until they align nicely with the part in the camera view. This is done using the **C virtual axis**. 

![Move Nozzle to Camera](https://user-images.githubusercontent.com/9963310/95973794-6580b280-0e14-11eb-98b1-8be29a4a5673.png) Now you can use the **Move Nozzle to Camera** button to move the nozzle back to the to the former camera coordinates, so not only X and Y are now applied to the nozzle, but also the safeguarded Z from before and the adjusted C. 

You can switch back and forth without losing coordinates. Capturing either the Nozzle or the Camera using the usual button (below) will get all the coordinates, thanks to the virtual axes. 

![Capture Buttons](https://user-images.githubusercontent.com/9963310/95981648-45a2bc00-0e1f-11eb-8493-73a13a5d3b91.png)


## Other Axes

Aside from the Machine Axes, discussed here, there are other types of axes, documented on their respective separate pages:

* To use shared axes, typically the shared Z of a multi-nozzle machine, you can use [[Transformed-Axes]]. 
* To compensate/calibrate for mechanical imperfections of the machine, use [[Linear-Transformed-Axes]].
