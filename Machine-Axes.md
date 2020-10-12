## The OpenPnP Coordinate System
OpenPnP conceptually uses a [Cartesian Coordinate System](https://en.wikipedia.org/wiki/Cartesian_coordinate_system). Assuming you stand in front of the machine, the X axis points to the right, Y away from you and Z up. The rotational axis C rotates around Z, counter-clockwise.

![Axis Coordinate System](https://user-images.githubusercontent.com/9963310/95686141-663efc00-0bfc-11eb-8a7a-153ab472f871.png)

The coordinate system now needs to be defined in OpenPnP. 

## Defining Axes
In the Machine Setup Tab hierarchical view, expand the Axes branch. Most likely you will already see defined axes, either migrated from an earlier version of OpenPnP or the default set.

### ReferenceControllerAxis

![Linear Axis Setup](https://user-images.githubusercontent.com/9963310/95686619-452bda80-0bff-11eb-89e2-26283b7fa8d9.png) 

Click on one of the existing ReferenceControllerAxis or create a new one using the ![Plus](https://user-images.githubusercontent.com/9963310/95689795-9f369b00-0c13-11eb-8347-7d4645776a0f.png) button and selecting the class:

![Select Axis Type](https://user-images.githubusercontent.com/9963310/95689720-0f90ec80-0c13-11eb-9c9d-aa33fd7888cf.png)

**Type** defines the axis meaning inside the Cartesian Coordinate System. OpenPnP can have multiple axes of the same Type, typically multiple Z and multiple C axes for multi-nozzle machines. 

![Axis Type](https://user-images.githubusercontent.com/9963310/95687314-10ba1d80-0c03-11eb-99d9-a638aecbf31b.png)

**Name** is your conceptual axis name. 

**Driver** links the axis to the driver which talks to the controller that controls its motion. 

**Axis Letter** gives you the letter of the axis by which it is addressed in the controller (usually a G-code letter). The letter must obviously be unique for a given driver. If you have multiple drivers, you typically have the same letter multiple times (and that is why inside OpenPnP you refer to the axis by its conceptual **Name** rather than its **Letter**). 

**Switch Linear ↔ Rotational**: G-code controllers make a difference between linear and rotational axes. The difference is relevant for the correct interpretation of feed-rates, acceleration limits and other issues. If you have a machine with more than one controller and/or more than two nozzles, chances are high that you are forced to be using a linear controller axis (typically X Y Z) to move what is conceptually a rotational axis (A B C) or vice versa. 
OpenPnP needs to know if this is the case, so use the switch to toggle the axis meaning. 

**Home Coordinate** gives OpenPnP the initial coordinate after homing. This may include a retract distance. 

**Backlash Compensation** will be treated later, on the [[Backlash-Compensation]] page. For the first basic setup leave it at **None**. 

**Resolution [Driver Units]** indicates the smallest step that OpenPnP should consider as a change in coordinate, also sometimes named a resolution "tick". It should be equal to a micro-step (or a practical multiple thereof). The larger the Resolution tick is, the more often OpenPnP can optimize axis moves away (especially in the small adjustments of non.squareness compensation, nozzle tip runout compensation etc.). By preventing unneeded micro axis move, OpenPnP can also prevent extra backlash compensation moves (not relevant with DirectionalCompensation). 


