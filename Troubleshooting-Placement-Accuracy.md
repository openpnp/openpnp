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

If you rotate the nozzle while it is in view of the bottom vision camera and you notice the nozzle center is moving away from the center-crosshair as you turn then your nozzle suffers from runout.

Consider enabling Bottom-Vision Pre-Rotation (every component is rotated to the desired placement rotation angle in bottom vision) by setting the checkbox in Machine Setup -> Vision -> Bottom Vision -> Rotate parts prior to vision?


# Bottom Vision

## Nozzle Center

## Units Per Pixel