## What is it?

Backlash compensation is used to avoid the effects of any looseness or play in the mechanical linkages of the given axis.  When the actuator reverses the direction of travel, there is often a moment where nothing happens, because the slack from a belt or play from a screw, rack&pinion etc. needs to be bridged, before mechanical force can again be transmitted.

![Wikipedia Backlash Illustration](https://user-images.githubusercontent.com/9963310/95771877-1de71300-0cbc-11eb-92f3-caa849999ade.png)

(From the Wikipedia page about [Backlash in Engineering](https://en.wikipedia.org/wiki/Backlash_(engineering))).

## Axis Configuration

Backlash compensation is configured on the ReferenceControllerAxis. Refer to the [[Machine-Axes]] page for general information. This page will focus on backlash compensation only.

**Note**: The following assumes you have your axes, driver etc. already set up and running i.e. you can jog around etc. and now you want to improve the accuracy of the machine. 

![Axis Wizard](https://user-images.githubusercontent.com/9963310/95772541-31df4480-0cbd-11eb-8086-c4106e24f103.png)

### Backlash Compensation Methods

**Backlash Compensation** defines the method used to compensate any backlash.


![Backlash Compensation](https://user-images.githubusercontent.com/9963310/95687283-e36d6f80-0c02-11eb-8d17-d3f2972c962b.png)


* **None:**
  No backlash compensation is performed. </li>
* **OneSidedPositioning:**
  Backlash compensation is applied by always moving to the end position from one side. 
  The backlash offset does not need to be very precise, i.e. it can be larger than the actual backlash and the machine will still end up in the correct precise position.
 The machine always needs to perform an extra move and it will force a complete machine still-stand between motion segments.</li>
* **OneSidedOptimizedPositioning:**
  Works like OneSidedPositioning except it will only perform an extra move when moving from the wrong side. Only half of the extra moves are needed.
* **DirectionalCompensation:**
Backlash compensation is applied in the direction of travel. The offset is added to the actual target coordinate, if moving in the direction of the offset (which can be positive or negative), no offset is added if moving against the offset. 
  No extra moves are needed. The machine can also move more fluidly, as there is no direction change needed. Seamless jogging is supported. 
  However: the **Backlash Offset** needs to precisely match the physical backlash.
* **DirectionalSneakUp:**
Works like **DirectionalCompensation** except for the last bit of the motion, where the speed is reduced. The idea is to have the machine arrive at the target in a similar state of agitation, regardless of how long and how fast the move was. This should create similar tensions in belts etc. and therefore create more repeatable positioning. 
  There are (mostly) no direction changes needed, but the two-step move is less fluid than with **DirectionalCompensation**. Seamless jogging is therefore not supported. 
  The **Backlash Offset** needs to precisely match the physical backlash.
  An additional **Sneak-up Distance** must be set.

**Backlash Offset** sets the amount of backlash. 

**Sneak-up Distance** sets the distance over which the **DirectionalSneakUp** method moves at reduced speed. If a move is smaller than that, but still going into the same direction as before, the sneak-up distance is reduced (in other words, the whole move is then done at the reduced speed). If a move is smaller, _and_ going into the other direction, it first backtracks further to allow for the full sneak-up distance.  

**Backlash Speed Factor** determines the speed factor of the final approach move in OneSided methods.

## Backlash Offset Calibration

**Note**, newer versions of OpenPnP provide [automatic Backlash calibration using Issues & Solutions](https://github.com/openpnp/openpnp/wiki/Calibration-Solutions#calibrating-backlash-compensation). 

![Backlash Calibration](https://user-images.githubusercontent.com/9963310/130323847-1a5ccfe6-072d-4f73-a64e-378749d936c7.png)

### Manual Calibration
____

**Note**, the following is only left for those that prefer a manual recipe. 
____

To determine the **Backlash Offset** for your X or Y axis, proceed as follows:

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
