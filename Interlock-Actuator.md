## What is it?
The InterlockActuator interlocks with axis movement. It can actuate itself according to specific axis positions or movements. Or it can read itself to confirm the safety of axis movement or lock against it, avoiding potentially dangerous machine situations. 

Example use cases:
* Actuate pneumatic nozzles up/down based on a virtual Z axis. 
* Prevent X, Y movement as long as a sensor indicates that the nozzles are not retracted.
* Prevent X, Y movement as long as a sensor indicates that a drag pin is not retracted, when it should.
* Confirm that motion has completed without any faults (steps lost fault signal etc.).
* Unlock a machine door when axes are parked. 
* Prevent axis movement when a door is sensed unlocked. 
* Signal a moving machine [e.g. for a Stack Light](https://en.wikipedia.org/wiki/Stack_light).

Many more are possible.

## Creating an InterlockActuator
In the Machine Setup go to the Head and add a new InterlockActuator:

![Creating an Interlock Actuator](https://user-images.githubusercontent.com/9963310/98390153-482ab700-2055-11eb-9ca5-557f82dd2e91.png)

## General Configuration 

![General Configuration](https://user-images.githubusercontent.com/9963310/98390535-cc7d3a00-2055-11eb-9095-ec8ac97056bf.png)

### Properties

**Driver** determines on which driver (i.e. the connected controller) the actuator is actuated or read. 

**Name** sets the name, by which the Actuator is identified (keep it stable, because Actuators are often referenced by name from other machine components). 

### Interlock Actuation

**Interlock Type** determines the central function of the actuator. 

![Interlock Type](https://user-images.githubusercontent.com/9963310/98391311-c471ca00-2056-11eb-88ce-85f303b3f158.png)

* **None**: The Actuator is effectively inactive.

* **Signal Axes Moving**: Signals itself boolean ON whenever _one or more_ of the given **Axes (1-4)** are moving. 

* **Signal Axes Standing Still**: Signals itself boolean ON whenever _all_ of the given **Axes (1-4)** are standing still. 

* **Signal Axes Inside Safe Zone**: Signals itself boolean ON whenever _all_ of the given **Axes (1-4)** are _inside_ their respective Safe Zones. See the [[Axis Safe Zone configuration for more information|Machine-Axes#kinematic-settings--axis-limits]]. This is typically used with a single Z axis and its Safe Z Zone. 

* **Signal Axes Outside Safe Zone**: Signals itself boolean ON whenever _one or more_ of the given **Axes (1-4)** are _outside_ their respective Safe Zones. 

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

![Condition State](https://user-images.githubusercontent.com/9963310/98395354-6ba53000-205c-11eb-8624-ba4791171ea9.png)

The two **Speed [%]** range limits determine under which machine speed range the interlock should apply. If the interlock is preventing potentially dangerous moves, you can still allow it to be overridden under low Speed [%]. 

![Speed Range](https://user-images.githubusercontent.com/9963310/98395737-fd14a200-205c-11eb-8291-afe001405e9e.png)

## Machine Coordination Considerations

The actuation and readings of an InterlockActuator must naturally be coordinated to the machine motion. This is configured with **Machine Coordination** [[(explained in more detail here)|Motion-Planner#actuator-machine-coordination]]. Usually the default settings are fine. 

![Machine Coordination](https://user-images.githubusercontent.com/9963310/98402881-5209e580-2068-11eb-9834-9a078893b4dc.png)

It must be mentioned that InterlockActuators will interrupt continuous [[Motion Path Planning|Motion-Planner#motion-path-planning]] resulting in a performance penalty and possibly preventing advanced optimizations such as [[Motion Blending|Motion-Planner#motion-blending]]. Therefore, these should only be used when necessary. 

## Useful Configuration Examples

### Pneumatic Nozzles

![Pneumatic Z](https://user-images.githubusercontent.com/9963310/98396119-89bf6000-205d-11eb-879d-a108bef3e59b.png)

Use this configuration to create a pneumatic nozzle (formerly known as a "Marek Nozzle"). Create a [[virtual Z axis|Machine-Axes#referencevirtualaxis]] and assign it to the Nozzle. Then create the InterlockActuator shown here to actuate the pneumatic valve to physically move as soon as the virtual axis reaches its Safe Z coordinate (which in case of a virtual axis is the same as the home coordinate).

### Safety Confirmation Sensor on a Z Axis

![Check Z Up](https://user-images.githubusercontent.com/9963310/98396529-2550d080-205e-11eb-9fd5-ee2ac5b7b102.png)

Use this configuration to prohibit any movement of the X, Y, C axes when the InterlockActuator reads itself outside the given confirmation range. 

### Safety Confirmation Sensor on a Drag Pin

![Drag Pin Confirm](https://user-images.githubusercontent.com/9963310/98399736-29cbb800-2063-11eb-8ff3-57e94c8a3d78.png)

Assume you have a DRAG_PIN actuator that engages the drag pin. Use this configuration to prohibit any movement of the X, Y, C axes when the InterlockActuator reads itself not matching the confirmation pattern. The interlock is subject to the DRAG_PIN actuator being switched off. This is obviously needed to allow the drag move itself. The example interlock makes an exception for very slow movement. This might be useful in order to try and gently de-block a stuck pin. 

### Handling a Safety Machine Door

a) ![Door Closed](https://user-images.githubusercontent.com/9963310/98400604-96938200-2064-11eb-9777-342bbcbebb66.png)
 b) ![Signal Parked](https://user-images.githubusercontent.com/9963310/98400561-824f8500-2064-11eb-8b07-e804a64ea16f.png)

The two InterlockActuators work together to a) only allow motion with a closed door confirmed by a sensor and b) unlock the door only when the axes are parked.  

