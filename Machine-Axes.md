## The OpenPnP Coordinate System
OpenPnP conceptually uses a [Cartesian Coordinate System](https://en.wikipedia.org/wiki/Cartesian_coordinate_system). Assuming you stand in front of the machine, the X axis points to the right, Y away from you and Z up. The rotational axis C rotates around Z, counter-clockwise.

![Axis Coordinate System](https://user-images.githubusercontent.com/9963310/95686141-663efc00-0bfc-11eb-8a7a-153ab472f871.png)

The coordinate system now needs to be defined in OpenPnP. 

## Defining Axes
In the Machine Setup Tab hierarchical view, expand the Axes branch. Most likely you will already see defined axes, either migrated from an earlier version of OpenPnP or the default set.

Click on the x or y axis:

![Linear Axis Setup](https://user-images.githubusercontent.com/9963310/95686619-452bda80-0bff-11eb-89e2-26283b7fa8d9.png) 

**Type** defines the axis meaning inside the Cartesian Coordinate System. OpenPnP can have multiple axes of the same Type, typically multiple Z and multiple C axes for multi-nozzle machines. 

![Axis Type](https://user-images.githubusercontent.com/9963310/95687314-10ba1d80-0c03-11eb-99d9-a638aecbf31b.png)

**Name** is your conceptual axis name. 

**Driver** links the axis to the driver which talks to the controller that controls its motion. 

**Axis Letter** gives you the letter of the axis by which it is addressed in the controller (usually a G-code letter). The letter must obviously be unique for a given driver. If you have multiple drivers, you typically have the same letter multiple times (and that is why inside OpenPnP you refer to the axis by its conceptual **Name** rather than its **Letter**). 

**Switch Linear ↔ Rotational**: G-code controllers make a difference between linear and rotational axes. The difference is relevant for the correct interpretation of feed-rates, acceleration limits and other issues. If you have a machine with more than one controller and/or more than two nozzles, chances are high that you are forced to be using a linear controller axis (typically X Y Z) to move what is conceptually a rotational axis (A B C) or vice versa. 
OpenPnP needs to know if this is the case, so use the switch to toggle the axis meaning. 

**Home Coordinate** gives OpenPnP the initial coordinate after homing. This may include a retract distance. 

**Backlash Compensation** will be treated in the [following section](Machine-Axes#backlash-compensation). For the first basic setup leave it at **None**. 

**Resolution [Driver Units]** indicates the smallest step that OpenPnP should consider as a change in coordinate, also sometimes named a resolution "tick". It should be equal to a micro-step (or a practical multiple thereof). The larger the Resolution tick is, the more often OpenPnP can optimize axis moves away (especially in the small adjustments of non.squareness compensation, nozzle tip runout compensation etc.). By preventing unneeded micro axis move, OpenPnP can also prevent extra backlash compensation moves (not relevant with DirectionalCompensation). 

## Backlash Compensation

**Backlash Compensation** defines the method used to compensate any backlash. **Note**: The following assumes you have your axes (and driver etc.) already set up and running i.e. you should first proceed with **None**, then come back later to choose the method and calibrate. 

![Backlash Compensation](https://user-images.githubusercontent.com/9963310/95687283-e36d6f80-0c02-11eb-8d17-d3f2972c962b.png)

Backlash compensation is used to avoid the effects of any looseness or play in the mechanical linkages of the given axis.  When the actuator reverses the direction of travel, there is often a moment where nothing happens, because the slack from a belt or play from a screw, rack&pinion etc. needs to be bridged, before mechanical force can again be transmitted.
* **None:**
  No backlash compensation is performed. </li>
* **OneSidedPositioning:**
  Backlash compensation is applied by always moving to the end position from one side. 
  The backlash offset does not need to be very precise, i.e. it can be larger than the actual backlash and the machine will still end up in the correct precise position.
 The machine always needs to perform an extra move and it will force a complete machine still-stand between motion segments.</li>
* **OneSidedOptimizedPositioning:**
  Works like OneSidedPositioning except it will only perform an extra move when moving from the wrong side. Only half of the extra moves are needed.
* **DirectionalCompensation (Experimental!):**
Backlash compensation is applied in the direction of travel. The offset is added to the actual target coordinate, if moving from below, no offset is added if moving from above. 
  No extra moves are needed. The machine can also move more fluidly, as there is no direction change needed. 
  However: the offset needs to precisely match the physical backlash.

**Backlash Offset** will set the amount of backlash. To calibrate X or Y, proceed as follows:

1. Move your down-looking camera to a location where you can precisely see even the tiniest moves (example: the homing fiducial). 
2. Set the Camera View to **Highest Quality (best scale)** (in the context menu) and zoom in using the scroll wheel of your mouse. 
3. Set the Machine Controls' Distance to the lowest (0.01mm).
4. Choose the **DirectionalCompensation** method.
5. Start with an Offset of zero (and Apply). 
6. Step in one direction and observe. The axis motor micro-step (or analogous) and the **Resolution [Driver Units]** defined earlier, will determine if the machine steps at all and how often. Stop, when a step was clearly visible.
  If your micro-stepping is < 0.01mm i.e. so fine that each step is moving the image, then just stop anywhere.
  If your micro-stepping is a non-integral multiple of 0.01mm then there may be smaller and larger steps, stop after a smaller step.
7. Now immediately reverse the direction. Count how many times you must step back, before the camera image starts to move in the other direction. 
8. If the first step back already moved the image then you don't need any compensation. Select **None**. Congratulations on a superb machine!
9. Otherwise, take the count, subtract 1 and multiply by 0.01mm and set it in the **Backlash Offset** (and Apply). 
10. Move back and forth and observe if the camera image really moves back and forth accordingly. Adjust the offset to optimize. Test at various positions along the axis. 
11. This recipe has now estimated the backlash offset for use with the **DirectionalCompensation** method. The true offset may vary with move speed and deceleration, position along the axis (length of belt), the temperature, wear and tear of the machine, the color of your underwear and whatnot. If the machine accuracy is unsatisfactory, you might have to consider using **OneSidedPositioning** method. Having said that, I had no problems whatsoever, to get very good and repeatable results on a simple belt machine (Liteplacer kit), using a 0.05mm backlash offset. So also check the machine, if results are bad.   
12. The same offset is also valid for the other methods, and you can increase it to be on the safe side. The **OneSidedPositioning** compensation will always move from the same side and use the same very low speed and deceleration to do so. This may result in more repeatable results. 
13. Please don't overestimate the precision that is needed. In reflow surface tension will drag parts into the correct position from surprisingly inaccurate placements. Roughly speaking, you can afford to place the finest pitch pads/pins/balls half off the correct solder land.
14. If you are using one of the OneSided methods, consider negating the offset: The offset should point in the direction, where extra movement is harmless, e.g. when applied inside the nozzle tip changer. However, you should never change the sign afterwards, as it may slightly affect the coordinate system. 

A similar method can be used to calibrate the C (rotational) axis. Pick a very large IC and use the bottom camera. 

It is not recommended (not useful) to compensate the Z axis, as nozzle tips are spring-loaded. 
