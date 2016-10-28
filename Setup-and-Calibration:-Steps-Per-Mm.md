# Steps Per Mm

Now that you can move the machine around and you have a camera it's a good time to check that your controller is moving the right amount when you tell it to. This is often part of your "steps per mm" configuration in most controllers. The way that you set this will be dependent on your controller, and you should check the instructions for the controller for more information.

## Testing Steps Per Mm

To test that your steps per mm settings are correct:

1. Place a [ruler](http://amzn.to/2642K3R) on the bed of the machine, along the X axis, so that it is visible to the camera. The ruler should be at least 100mm in length. The closer it is to the length of your axes the better your accuracy will be.
2. Jog the camera so that it is focused on the edge of the ruler.
3. Jog the camera back and forth along the length of the ruler and adjust the ruler on the bed until the camera crosshair tracks the edge perfectly.
4. Align the camera crosshair with one of the millimeter lines on the left of the ruler.
5. Jog 100mm to the right.
6. Check if the crosshairs now line up with the millimeter marking 100mm from where you started. If they do not, you will need to adjust your steps per mm setting. If the crosshairs went past 100mm then your steps per mm setting is too high. If they did not get all the way to the 100mm marking then the setting is too low.
7. When your X axis is adjusted correctly, perform the same test with the Y axis by turning the ruler 90 degrees.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Driver Setup|Setup and Calibration: Top Camera Setup]] | [[Table of Contents|Setup and Calibration]] | [[Nozzle Setup|Setup and Calibration: Nozzle Setup]] |