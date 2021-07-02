## The OpenPnP Coordinate System
OpenPnP conceptually uses a [Cartesian Coordinate System](https://en.wikipedia.org/wiki/Cartesian_coordinate_system). Assuming you stand in front of the machine, the X axis points to the right, Y away from you and Z up. The rotational axis C rotates around Z. When looking down on the machine table, this is counter-clockwise.

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

### Controller Settings (Rotational Axis)

A rotational axis has no Limits but two more checkboxes. 

![Rotation Axis](https://user-images.githubusercontent.com/9963310/96336707-02945300-1082-11eb-9095-0e7c6985d65d.png)

**Limit to ±180°** keeps the rotation within the range -180° ... +180°. Instead of e.g. going to 190°, it would go to the equvalent -170° position. Some retrofitted machines have physically limited axes where this setting can be helpful. 

**Wrap around** will always go the shorter way around to the desired angle. Instead of e.g. going from 270° to 30° (-240° turn) it will go to the equvalent 390° position (+120° turn). This may result in the angle winding up to very large numbers. In some contexts the machine will then unwind all at once, possibly causing undue strain on the vacuum tube coupling. 

Fortunately, if **Limit to ±180°** and **Wrap around** are combined, the axis coordinate is reset to its -180° ... +180° equivalent coordinate after the wrap-around move. Therefore the angle will not wind up. 

Note: the GcodeDriver must have the `SET_GLOBAL_OFFSETS_COMMAND` configured. See the [[migration guide|Advanced-Motion-Control#migration-from-a-previous-version]]. The controller must support using G92 (or equivalent) on the fly, i.e. while the previous move command may still be in the process of being executed (if not, add M400 before the G92, but be aware that this disrupts fluid motion). For Smoothieware, you must use [my special PnP firmware](https://makr.zone/smoothieware-new-firmware-for-pnp/500/) for this to work correctly ([reasons explained here](https://github.com/Smoothieware/Smoothieware/pull/1487)). 

### Kinematic Settings / Axis Limits

**Soft Limit Low** and **Soft Limit High** bracket the valid range of the axis. OpenPnP will refuse a move to a Location if it leads outside this range. 

![Soft Limit](https://user-images.githubusercontent.com/9963310/95889274-7e408800-0d82-11eb-9b45-d275c7d96874.png)

The respective Limit is only enforced if **Enabled?** is selected. 

![Capture Axis Limit](https://user-images.githubusercontent.com/9963310/95889746-1c345280-0d83-11eb-8386-07d856e85958.png) Use the **Capture** button to capture the current axis position as the new limit.

![Position to Axis Limit](https://user-images.githubusercontent.com/9963310/95889714-0e7ecd00-0d83-11eb-8945-e49f43e80d5b.png) Use the **Position** button to move the axis to its current limit. 

Within the soft limits, you can define the **Safe Zone Low** and **Safe Zone High** limits. This is mandatory for the Z axis, to define the so-called **Safe Z**. To avoid collisions with anything on the machine table, Nozzles (and other so-called Head-Mountables) are lifted to **Safe Z** before the Head is allowed to move in X and Y (or C). 

The **Safe Zone** can be the same value for **Low** and **High**, defining a single **Safe Z** height. Or it can be a range, to give OpenPnP more freedom to optimize its operation. 

Multi-nozzle machines often share one physical Z axis to make two Nozzles move in a counterweight, seesaw or rocker configuration (see the [[Transformed-Axes]] page). If you want your **Safe Z** at the perfect balance-point of the two Nozzles, choose the mid-point of the Z axis as the **Safe Z** height, i.e. **Low/High** at the same value. You should also use this setting as a starting point, if you are still in the process of defining the axes for the first time (chicken egg problem). 

If your machine is already setup, you can revisit the Z axis and optimize. The Nozzles may have a lot of Z headroom, i.e. it is quite slow to move them all the way to the balance-point. So you can choose individual **Safe Z** heights for the Nozzles and set them as the **Safe Zone Low** and **High** limits respectively (you will see which is which when you Jog them up and down). OpenPnP is then free to optimize and only lift the nozzle up as far as needed. But be prepared for the laid-back "hang loose" motion style ;-) 

If there is a switch of nozzles (one going up, move in X/Y, the _other_ going down), OpenPnP will even exploit the time in transit to move the Z axis over to the _other_ limit of the Safe Zone, readying the _other_ Nozzle at the exit point for the quickest/shortest descent to the target location.  

The other linear axes (X, Y) may also have a limited **Safe Zone**. If you combine the Safe Zone out of all the axes, you get a box in space that is considered safe for any motion (no collisions). Inside that box, OpenPnP is free to do any motion. This is exploited for [[Advanced-Motion-Control]]. 

Note: if a Safe Zone Limit is not defined (**Enabled?** switches off), it is considered unlimited, i.e. _any_ position is considered safe, as long as the other axes are (i.e. at least the Z axis) are in their respective Safe Zones . 

### Kinematic Settings / Rate Limits

**Feedrate [/s]** sets the speed limit on the axis. **NOTE**: Unlike in G-code `F` words, this limit is per second rather than per minute. It seemed more consistent to define all rates (Feedrate/Acceleration/Jerk) in the same [s] time unit. You need to divide by 60 if converting from G-code feed-rates. 

The Axes can have different speed limits which is especially important for the rotational axes where the limit is angular [°/s] rather than linear [mm/s]. In typical DIY stepper machines the angular speed can be much higher than the linear speed, e.g. a 180° nozzle turn should be quicker than a 180mm linear move. 

When OpenPnP is performing a move with several axes involved (e.g. a diagonal plus rotation), it will exploit the speed limit on all the axes. I.e. the overall speed along the diagonal can be significantly faster than for an individual axis (this behavior is optional, see [[Advanced-Motion-Control]]). 

**Acceleration [/s²]** sets the [acceleration](https://simple.wikipedia.org/wiki/Acceleration) limit (maximum change of velocity per unit of time). In many cases this is more important than the speed limit, as the speed limit is only ever reached in very long moves. 

**Jerk [/s³]** sets the [jerk](https://simple.wikipedia.org/wiki/Jerk) limit (maximum change of acceleration per unit of time). If left at zero, no jerk control will be used. Without jerk control, the acceleration will be switched on and off instantaneous, creating vibrations and wear and tear. OpenPnP has several options to use jerk control, even if your controller does not natively have the capability (see [[Advanced-Motion-Control]]).

**Important**: you need to set these limits **at or below** the limits configured for the controller. Otherwise you will get very strange result, as the motion planning is then completely useless. 

In case of Smoothieware, look for values like these (divide by 60 from mm/min). See the [Smoothie guide](http://smoothieware.org/configuration-options): 

<pre>
x_axis_max_speed                             40000            # mm/min
alpha_max_rate                               40000            # mm/min actuator max speed
alpha_acceleration                           2000.0           # mm/sec^2
</pre>

#### What values should I set?

This question is impossible to answer, as each machine is different. The problem is further confounded, as the two or three rate limits play together. The following is not quite a recipe but gives you some ideas on how to approach good values for your machine. This is done **per Axis**:

1. Backup your configuration, both for OpenPnP and your controller. Note down the [Park location](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Park-Location).
2. Set very high limits in your controller configuration, so you are free to experiment on the OpenPnP side. The controller must not clip the rates you are setting in OpenPnP. Be extra sure this is the case, otherwise you can easily fool yourself in the steps below, and things can become very confusing. 
3. If you have Duet, Smoothieware or another high performance controller, set your driver to **Simulated3rdOrderControl**. If you have TinyG and other supported S-Curve controllers, set **SimpleSCurve**. Otherwise set **ModeratedConstantAcceleration**. See [here for how to](https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings).
4. Set your [Park location](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Park-Location) to some prominent high-contrast mark on your machine table, somewhere in the middle, where you can move freely in all directions. You need to be able to judge whether the mark is precisely in the cross-hairs of your down-looking camera, after pressing **`P`**. This will be your check for the machine not having lost steps (assuming you don't have closed loop). 
5. Go to Machine/Setup down-looking camera, into the **Vision** tab. If [[Camera Settling]] is not yet configured, enable it like so:
   
   ![Camera Settle Diagnostics](https://user-images.githubusercontent.com/9963310/124281237-d1917b00-db49-11eb-9147-5c3e56c350d9.png)

6. Better yet: do the [[Camera Settling]] setup right now (if you haven't already) and you will get the settling times as an additional _valuable_ indicator. You may see a trade-off between aggressive machine motion and short settling times, because of shaking/vibrations. For simpler machines such as the Liteplacer, this is a significant trade-off i.e time saved on break-neck moves may easily be wasted many times over in longer settle times. Note that long settle times also mean less precision in picking/placing as the nozzle tip may be swinging all over the place for several hundred milliseconds. 
7. Start with feed-rate 200mm/s, acceleration 500mm/s² and jerk 10000mm/s³ on **SimpleSCurve** controllerd, jerk 0mm/s³ (disabled) on other controllers (see step 3).
8. Set different step distances (1mm, 10mm, 100mm) in the Machine Controls and for each of those do the following:
   
   ![Distance Control](https://user-images.githubusercontent.com/9963310/124281344-ea9a2c00-db49-11eb-8dde-19c075f71457.png)

9. Use the arrow button in the **Vision** tab (_not_ the Machine Controls) to test machine motion. You should then get a graphical diagnostic of how your machine shakes/vibrates, plus (as discussed in step 6) the settle times. 
10. Also note the sound of the motors. Try to get a feel when the motors reach their limits. Check with **`P`** as prepared in step 4 to make sure no steps are lost. 
11. Increase acceleration on the axis and repeat from step 8 until you think you reach a critical limit/trade-off. Increase feed-rate, when the speed levels out on long moves. CAUTION: for each increase of feed-rate, you also need to check if very long moves still work. For **SimpleSCurve** controllers, if things _still_ level out, proceed to the next step immediately. You can also use [Motion Planner Diagnostics](https://github.com/openpnp/openpnp/wiki/Motion-Planner#motion-planner-diagnostics) to see _where_ and _when_ which limits shapes the motion profile. 
    
    ![Motion Profile](https://user-images.githubusercontent.com/9963310/96153482-0dc67200-0f0e-11eb-8d6e-fe7ac8a249eb.png)

12. Once you think you've got good acceleration governed performance, you can start experimenting with jerk. For **SimpleSCurve** controllers this is actually the main tuning knob. For those with **ModeratedConstantAcceleration**, it will dampen acceleration on short moves. For those with **Simulated3rdOrderControl**, a jerk limit is only simulated and subject to good interpolation settings, i.e. this is now the time for you to make sure, the [Interpolation](https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#interpolation) is set up correctly.
13. When a jerk limit is not provided, most controllers act with the so-called Constant Acceleration model. This effectively means that jerk will be infinite (in theory). By limitting jerk we can greatly reduce vibrations. As we're going from "infinite", the numbers can be quite hight. You can start from 100000mm/s³ and then go down. 
14. The jerk limit will ease the aggressiveness of acceleration both on start/stop but also at the top speed transition. Both allows you to increase the acceleration, and sometimes even the feed-rate limit in turn. By experimenting with different jerk/acceleration combinations, you can find better trade-offs with camera settling, as discussed in step 6. 
15. Do this over and over again. Note down good settings. 
16. Once you get good settings on all axes, set your controller config limits to the same or somewhat higher values (for safety).
17. If you have not done proper [[Camera Settling]] setup in steps 5 and 6, switch it back to **FixedTime**. 
18. Restore the [Park Location](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Park-Location) to what it was before.

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

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [Driver Setup](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Driver-Setup) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Top Camera Setup](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Top-Camera-Setup) |

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

