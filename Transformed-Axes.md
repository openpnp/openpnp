## What is it?

Transformed axes take one [[Machine Axis|Machine-Axes]] as input and transform the coordinate into a new one. We are discussing these transformations as **forward transformations** here, in the sense as they are happening mechanically, from the raw actuator (motor) coordinate to the target coordinate for the machine component that is moved. 

But of course, OpenPnP will internally also derive the _reverse_ transformations, from the desired target coordinate for the machine component to be moved, _back_ to the raw coordinate of the actuator (motor). However, you need not concern yourself with those (sometimes difficult) _reverse_ transformations. 

## ReferenceMappedAxis

The ReferenceMappedAxis can take two coordinate points A and B on the _input_ axis and map those to two corresponding _output_ coordinate points. Any input coordinate is then proportionally interpolated/extrapolated using these two map points. 

Using the ReferenceMappedAxis, you can scale, offset, negate or reverse any axis (and more). 

### Create an Axis

Create an axis in the Machine Setup tab.

![Create Mapped Axis](https://user-images.githubusercontent.com/9963310/95983710-67517280-0e22-11eb-9d90-ef4526970692.png)

### Properties

![ReferenceMappedAxis](https://user-images.githubusercontent.com/9963310/95984469-87cdfc80-0e23-11eb-9f43-2a83b906632e.png)

**Type** and **Name** are the same as in the [[ReferenceControllerAxis|Machine-Axes#properties]]. 

The **Type** will restrict the input axes to the same **Type** i.e. only the coordinates are mapped, the geometric axis itself (dimension in space) remains the same. 

### Axis Mapping

**Input Axis** denotes the axis where the raw input coordinate is taken from. 

**Map Point A** and **Map Point B** define the two mapped coordinate points. To explain let's make a few examples:

### Negate an Axis

The zero point (origin) of the axis remains the same, but the unit 1 is mapped to unit -1, so proportionally any coordinate is mapped to its negative value. 

![Negate](https://user-images.githubusercontent.com/9963310/95986818-dcbf4200-0e26-11eb-82a2-c91fb1866b10.png)

This mapping can be used for a Rack&Pinion shared Z axis a.k.a. "Peter's Head". If one Nozzle goes up (+Z), the other goes down (-Z).

![Peters Head](https://user-images.githubusercontent.com/9963310/95987574-e8f7cf00-0e27-11eb-9d89-4a538268eea3.png)

Image: [Pick and place head dual nozzle by Betz Technik](https://www.betztechnik.ca/store/p34/Pick_and_Place_head_-_dual_nozzle_-_OpenPnP_compatible.html#).

### Reverse an Axis

If for some reason you have an axis that goes the wrong way, you can reverse it by mapping the axis coordinate maximum to the coordinate minimum and vice versa. The coordinates stay positive.

![Reverse Axis](https://user-images.githubusercontent.com/9963310/95988924-dbdbdf80-0e29-11eb-9fcb-c5c91d9a3944.png)

### Offset an Axis

The following example adds 100mm to all the raw axis coordinates:

![Offset Axis](https://user-images.githubusercontent.com/9963310/95989202-3e34e000-0e2a-11eb-9b36-6d62f25ab7e9.png)

### Scale an Axis

As a fantasy example, assume you have salvaged a controller from a printer that works in [Twips](https://en.wikipedia.org/wiki/Twip). We therefore need to map 1440 twips to one inch but in the Driver's length unit Millimeters (25.4mm):

![Scaled Axis](https://user-images.githubusercontent.com/9963310/95990392-bcde4d00-0e2b-11eb-82d7-97d6c6337412.png)

## ReferenceCamCounterClockwiseAxis

The ReferenceCamCounterClockwiseAxis is used to transform shared Z axis that work in a seesaw or rocker configuration. 

![Cam Counter-Clockwise](https://user-images.githubusercontent.com/9963310/128175722-96536664-415b-4ba1-a987-dbb196a96aea.png)

**Type** and **Name** are the same as in the [[ReferenceControllerAxis|Machine-Axes#properties]]. 

The **Type** will restrict the input axes to the same **Type** i.e. only the coordinates are mapped, the geometric axis itself (dimension in space) remains the same. 

**Input Axis** denotes the axis where the raw input coordinate is taken from. 

**Cam Radius** is the important parameter of this transformation. It defines the leverage with which the angular motion of the motor translates into the linear motion of the two nozzles that are typically attached to precision linear rails. One nozzle is pressed down, while the other is typically pulled up using a spring. 

**Cam Arms Angle** defines the angle between the two arms. Normally, a 180° angle is used for straight-across arms. If the angle is 0°, it describes a one-armed design that always only pushes one side i.e nozzle at a time. Other "V-shaped" arms angles could be used to describe designs, that eliminate dead-time, that the 0° one-armed design has.

**Cam Wheel Radius** and **Cam Wheel Gap** are physical properties of the mechanics. However, they both just add a constant offset to the transformation. Because we relate the target Z coordinate to the nozzle **tip** rather than the nozzle **back** (where the cam wheel pushes it), such an offset is not purposeful and you will simply end up compensating for it in the nozzle offset. ⚠ It is highly recommended to **leave both at zero**.

## ReferenceCamClockwiseAxis

The ReferenceCamCounterClockwiseAxis is really easy. It just takes its sibling ReferenceCamCounterClockwiseAxis as input, the actual transformation is already parametrized there.

![Cam Clockwise](https://user-images.githubusercontent.com/9963310/95995093-56f4c400-0e31-11eb-8b15-f90cd5e171d7.png)

## Using the Transformed Axis

The created axis can now be used like a physical axis when it comes to [[Mapping Axes]].

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
