# Work in Progress
This page is not yet complete. 

If your machine is up and running but your placements are not as accurate as you expect them to be, try the steps below to determine the cause and then see below for some potential solutions.

# X/Y Accuracy
1. Perform a fiducial check on your board.
2. Once complete, select the first fiducial in the placements list and click the "Move Camera" button. The camera should be well centered over the fiducial.
3. Now select the second fiducial and do the same thing.
4. If either fiducial was not well centered the following issues could be present:
   * Steps-per-mm is slightly off: https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Steps-Per-Mm
   * Consider non-squareness compensation (https://github.com/openpnp/openpnp/wiki/GcodeDriver#non-squareness-compensation)
   * consider enabling backlash compensation: https://github.com/openpnp/openpnp/wiki/GcodeDriver#backlash-compensation
   * Make sure the PCB is lying 100% flat, very small deviations can already be visible if the top vision camera has high resolution and is close to the PCB

# Nozzle Runout

If you rotate the nozzle while it is in view of the bottom vision camera and you notice the nozzle center is moving away from the center-crosshair then your nozzle suffers from runout.

Consider enabling Bottom-Vision Pre-Rotation (every component is rotated to the desired placement rotation angle in bottom vision) by setting the checkbox in Machine Setup -> Vision -> Bottom Vision -> Rotate parts prior to vision?

Note that C axis in most machines cannot be homed so nozzle runout will affect the nozzle rotation/position being different every time the machine is turned on. A manual workaround here is to mark the nozzle with a label on one side and make sure it is oriented the same way every time the machine is turned on and homed.

# Head Offset

If all placements on a board are slightly offset in the same direction this is a good indicator that either the head-camera offset is wrong: https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Top-Camera-Setup#head-offsets or the bottom vision nozzle position is off (see below: Nozzle Center)

# Bottom Vision


## Camera Orientation

The correct orientation for the bottom vision camera can be verified by moving the nozzle in view with the jog controls. This may sound obvious but pressing the jog-up button should move the nozzle upwards in view, pressing jog-left should move the nozzle left in view. As the bottom vision camera settings allow rotation and flipping the image on both axes this can quickly create a confusingly oriented image that will result in placement offsets going in the wrong directions.

## Nozzle Center

Even with accurate end stops plus optical homing of the top vision camera we see that the bottom vision nozzle location can slightly vary with every homing run. Since the nozzle center being slightly off will result in all placements being slightly off it is highly recommended to verify and if necessary correct the bottom vision nozzle center location after every homing run.

## Units Per Pixel

If this value is not accurate it will result in bottom vision component offsets being under or overcompensated. Looking at vision debug images and resulting placement locations can give a clue here.