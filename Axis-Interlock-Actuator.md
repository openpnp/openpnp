## What is it?
An axis interlock actuator can automatically act when axis movement happens. It can actuate itself according to specific axis positions or movements, or it can read itself to confirm the safety of axis movement and lock against it, avoiding potentially dangerous machine situations. 

Example use cases (some are [documented below](#useful-configuration-examples)):
* Actuate pneumatic nozzles up/down based on a virtual Z axis. 
* Prevent X, Y movement as long as a sensor indicates that the nozzles are not retracted.
* Prevent X, Y movement as long as a sensor indicates that a drag pin is not retracted, when it should.
* Confirm that motion has completed without any faults (steps lost fault signal etc.).
* Unlock a machine door when axes are parked. 
* Prevent axis movement when a door is sensed unlocked. 
* Signal a moving machine [e.g. for a Stack Light](https://en.wikipedia.org/wiki/Stack_light).

Many more are possible.

## Creating an Actuator for Axis Interlock
In the Machine Setup go to the Head and add a new Actuator using the `+` button. Any of the Actuator implementation can be used for axis interlock functions. Refer to the [[Setup and Calibration: Actuators]] page for more information about how the actuators are set up. This page will only discuss the extra Axis Interlock functionality.

![Creating an Interlock Actuator](https://user-images.githubusercontent.com/9963310/98463289-6f4ec900-21ba-11eb-969b-2b20a9265c07.png)

Enable the **Axis Interlock** functionality and press Apply.

![Enabling the Axis Interlock](https://user-images.githubusercontent.com/9963310/98463375-1e8ba000-21bb-11eb-91fd-5c23bade7c9c.png)

A new **Axis Interlock** Wizard tab will appear. 

## General Configuration 

![General Configuration](https://user-images.githubusercontent.com/9963310/98463419-73c7b180-21bb-11eb-93ec-66dcb58cd22f.png)

### Axis Interlock

**Interlock Type** determines the central function of the actuator. 

![Interlock Type](https://user-images.githubusercontent.com/9963310/98463868-e4bc9880-21be-11eb-82c8-1bf68c61fe63.png)

* **None**: The Actuator is effectively inactive.

* **Signal Axes Moving**: Signals itself boolean ON whenever _one or more_ of the given **Axes (1-4)** are moving. 

* **Signal Axes Standing Still**: Signals itself boolean ON whenever _all_ of the given **Axes (1-4)** are standing still. 

* **Signal Axes Inside Safe Zone**: Signals itself boolean ON when _all_ of the given **Axes (1-4)** are moving _inside_ their respective Safe Zones. See the [[Axis Safe Zone configuration for more information|Machine-Axes#kinematic-settings--axis-limits]]. This is typically used with a single Z axis and its Safe Z Zone (see the [example below](#pneumatic-nozzles)). The actuation happens before the move if it goes into the Safe Zone. The actuation happens after the move, if it goes away from the Safe Zone. 

* **Signal Axes Outside Safe Zone**: Signals itself boolean ON when _one or more_ of the given **Axes (1-4)** are moving _outside_ their respective Safe Zones. 

* **Signal Axes Parked**: Signals itself boolean ON whenever _all_ of the given **Axes (1-4)** are in their Park position. This is the position the machine goes to, when pressing the corresponding `[P]` buttons on the Machine Controls. 

* **Signal Axes Unparked**: Signals itself boolean ON whenever _one or more_ of the given **Axes (1-4)** are _not_ in their Park position.  

* **Confirm in Range Before Axes Move**: Before one or more of the given **Axes (1-4)** are about to be moved, the actuator reads itself as a numeric value (floating point value) and compares the reading to the **Confirmation range**. Two extra fields appear, where you can enter the lower and upper limits of the range. If the reading is within that range, the axis move is allowed. Otherwise, the current Job or user Action will be interrupted. 

  ![Confirmation Range](https://user-images.githubusercontent.com/9963310/98393188-3ba85d80-2059-11eb-92d2-623cb08cd226.png)

* **Confirm in Range After Axes Move**: After _one or more_ of the given **Axes (1-4)** have been moved, the actuator reads itself as a numeric value (floating point value) and compares the reading to the **Confirmation range**. If the reading is within that range, further operation is allowed. Otherwise, the current Job or user Action will be interrupted. 

* **Confirm Match Before Axes Move**: Before _one or more_ of the given **Axes (1-4)** are about to be moved, the actuator reads itself as a text value and compares the reading to the **Confirmation pattern**. Two extra fields appear, where you can enter the pattern and the option to match it using [Regular Expressions syntax](https://en.wikipedia.org/wiki/Regular_expression). If the reading is a match, the axis move is allowed. Otherwise, the current Job or user Action will be interrupted. 

  ![Confirmation Pattern](https://user-images.githubusercontent.com/9963310/98394542-38ae6c80-205b-11eb-990e-48aea81dd7d6.png)

* **Confirm Match After Axes Move**: After _one or more_ of the given **Axes (1-4)** have been moved, the actuator reads itself as a text value and compares the reading to the **Confirmation pattern**. If the reading is a match, further operation is allowed. Otherwise, the current Job or user Action will be interrupted. 

### Interlock Conditions

The interlock function described above is _subject to_ the conditions you can define here. 

**Boolean Actuator** determines which actuator's last actuation state to consider as a precondition to apply the interlock function. As soon as you select an actuator, an additional field appears to select the state wanted for the interlock to happen. When OpenPnP is started or after a homing operation, the actuation state is considered unknown. You can choose to include or exclude the unknown state:  

![Condition State](https://user-images.githubusercontent.com/9963310/98463636-ea18e380-21bc-11eb-9f35-0e79cbdb7a5a.png)

The two **Speed [%]** range limits determine under which machine speed range the interlock should apply. If the interlock is preventing potentially dangerous moves, you can still allow it to be overridden under low Speed [%]. 

![Speed Range](https://user-images.githubusercontent.com/9963310/98395737-fd14a200-205c-11eb-8291-afe001405e9e.png)

### Machine Coordination 

The actuation and readings of an InterlockActuator must naturally be coordinated to the machine motion. This is configured with **Machine Coordination** [[(explained in more detail here)|Motion-Planner#actuator-machine-coordination]]. Usually the default settings are fine. 

![Machine Coordination](https://user-images.githubusercontent.com/9963310/98403934-048e7800-206a-11eb-988e-212ac69e887e.png)

It must be mentioned that InterlockActuators will interrupt continuous [[Motion Path Planning|Motion-Planner#motion-path-planning]] resulting in a performance penalty and possibly preventing advanced optimizations such as [[Motion Blending|Motion-Planner#motion-blending]]. Therefore, these should only be used when necessary. 

## Useful Configuration Examples

### Pneumatic Nozzles

![Pneumatic Z](https://user-images.githubusercontent.com/9963310/98463670-25b3ad80-21bd-11eb-96bf-3f56d9fab4a1.png)

Use this configuration to create a pneumatic nozzle (formerly known as a "Marek Nozzle"). Create a [[virtual Z axis|Machine-Axes#referencevirtualaxis]] and [[map it to the nozzle|Mapping Axes]]. Then create the InterlockActuator shown here to actuate ON the pneumatic valve to physically move up the nozzle when the virtual axis is about to move to its Safe Z coordinate (actuation _before_ the move). It will actuate OFF to move the Nozzle down when the virtual Z axis has completed a move that leaves the Safe Z coordinate (actuation _after_ the move). 

**Notes:** 
* In case of a  [[virtual axis|Machine-Axes#referencevirtualaxis]] the Safe Z coordinate is the same as the home coordinate. 
* The _before/after_ move characteristic is only relevant if other axes than Z are moved at the same time, for example a diagonal move, in a nozzle tip changing move. You should then of course be aware of the order and timing with which these physical movements happen. 
* You might want to enable the [Machine Coordination](#machine-coordination) **After Actuation** option, if the pneumatic action takes a considerable amount of time to complete and its completion is acknowledged by the controller's flow-control, `M400` or similar.

### Safety Confirmation Sensor on a Z Axis

![Check Z Up](https://user-images.githubusercontent.com/9963310/98463707-8642ea80-21bd-11eb-8138-6d573e14144e.png)

Use this configuration to prohibit any movement of the X, Y, C axes when the InterlockActuator reads itself outside the given confirmation range. 

### Safety Confirmation Sensor on a Drag Pin

![Drag Pin Confirm](https://user-images.githubusercontent.com/9963310/98463716-a7a3d680-21bd-11eb-9972-07e405a58e02.png)

Assume you have a DRAG_PIN actuator that engages the drag pin. Use this configuration to prohibit any movement of the X, Y axes when the InterlockActuator reads itself not matching the confirmation pattern. The interlock is subject to the DRAG_PIN actuator being switched off. This is obviously needed to allow the drag move itself. The example interlock makes an exception for very slow movement. This might be useful in order to try and gently de-block a stuck pin. 

### Handling a Safety Machine Door

a) ![Door Sensor](https://user-images.githubusercontent.com/9963310/98463785-429cb080-21be-11eb-8c2a-ef8d93357116.png)
 b) ![Door Lock](https://user-images.githubusercontent.com/9963310/98463814-8394c500-21be-11eb-837f-aa640bf3525c.png)

The two axis interlock actuators work together to a) only allow motion with a closed door confirmed by a sensor and b) lock the door as soon as the axes move away from being parked.  

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
