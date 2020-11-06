## What is it?
The InterlockActuator interlocks with axis movement. It can actuate itself according to specific axis positions or movements. Or it can read itself to confirm the safety of axis movement or lock against it, avoiding potentially dangerous machine situations. 

Use cases:
* Actuate pneumatic nozzles up/down based on a virtual Z axis. 
* Prevent X, Y movement as long as a sensor indicates that the nozzles are not retracted.
* Prevent X, Y movement as long as a sensor indicates that a drag pin is not retracted, when it should.
* Confirm that motion has completed without any faults (steps lost fault signal etc.).
* Unlock a machine door when axes are parked. 
* Prevent axis movement when a door is sensed unlocked. 
* Signal a moving machine (safety indicator).

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

* **Signal Axes Moving**: Signals itself boolean ON whenever one or more of the given **Axes (1-4)** are moving. 

* **Signal Axes Standing Still**: Signals itself boolean ON whenever one _all_ of the given **Axes (1-4)** are standing still. 

* **Signal Axes Inside Safe Zone**: Signals itself boolean ON whenever all of the given **Axes (1-4)** are _inside_ their respective Safe Zones. See the [[Axis Safe Zone configuration for more information|Machine-Axes#kinematic-settings--axis-limits]]. This is typically used with a single Z axis and its Safe Z Zone. 

* **Signal Axes Outside Safe Zone**: Signals itself boolean ON whenever one or more of the given **Axes (1-4)** are _outside_ their respective Safe Zones. 

* **Signal Axes Parked**: Signals itself boolean ON whenever all of the given **Axes (1-4)** are in their Park position. This is the position the machine goes to, when pressing the corresponding `[P]` buttons on the Machine Controls. 

* **Signal Axes Unparked**: Signals itself boolean ON whenever one or more of the given **Axes (1-4)** are _not_ in their Park position.  

* **Signal Axes Unparked**: Signals itself boolean ON whenever one or more of the given **Axes (1-4)** are _not_ in their Park position.  

* **Confirm in Range Before Axes Move**: Before one or more of the given **Axes (1-4)** are about to be moved, the actuator reads itself as a numeric value (floating point value) and compares the reading to the **Confirmation range**. Two extra fields appear, where you can enter the lower and upper limits of the range. If the reading is within that range, the axis move is allowed. Otherwise, the current Job or user Action will be interrupted. 

  ![Confirmation Range](https://user-images.githubusercontent.com/9963310/98393188-3ba85d80-2059-11eb-92d2-623cb08cd226.png)

* **Confirm in Range After Axes Move**: After one or more of the given **Axes (1-4)** have been moved, the actuator reads itself as a numeric value (floating point value) and compares the reading to the **Confirmation range**. If the reading is within that range, further operation is allowed. Otherwise, the current Job or user Action will be interrupted. 

* **Confirm Match Before Axes Move**: Before one or more of the given **Axes (1-4)** are about to be moved, the actuator reads itself as a text value and compares the reading to the **Confirmation pattern**. Two extra fields appear, where you can enter the pattern and the option to match it using [Regular Expressions syntax](https://en.wikipedia.org/wiki/Regular_expression). If the reading is a match, the axis move is allowed. Otherwise, the current Job or user Action will be interrupted. 

  ![Confirmation Pattern](https://user-images.githubusercontent.com/9963310/98394542-38ae6c80-205b-11eb-990e-48aea81dd7d6.png)

* **Confirm Match After Axes Move**: After one or more of the given **Axes (1-4)** have been moved, the actuator reads itself as a text value and compares the reading to the **Confirmation pattern**. If the reading is a match, further operation is allowed. Otherwise, the current Job or user Action will be interrupted. 





