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

# Component Rotation Issue

Small nozzle tips can also pick up larger components (chips or 1206) but have trouble rotating their heavier mass as quickly as with smaller components which can lead to larger components being placed slightly off angle. Consider using a larger nozzle tip for larger components.

If you see off angle components also with smaller parts consider enabling Bottom-Vision Pre-Rotation (every component is rotated to the desired placement rotation angle in bottom vision) by setting the checkbox in Machine Setup -> Vision -> Bottom Vision -> Rotate parts prior to vision?

# Nozzle Runout

If you rotate the nozzle while it is in view of the bottom vision camera and you notice the nozzle center is moving away from the center-crosshair then your nozzle suffers from runout.

Consider enabling Bottom-Vision Pre-Rotation (every component is rotated to the desired placement rotation angle in bottom vision) by setting the checkbox in Machine Setup -> Vision -> Bottom Vision -> Rotate parts prior to vision?

Note that C axis in most machines cannot be homed so nozzle runout will affect the nozzle rotation/position being different every time the machine is turned on. A manual workaround here is to mark the nozzle with a label on one side and make sure it is oriented the same way every time the machine is turned on and homed.

# Head Offset

If all placements on a board are slightly offset in the same direction this is a good indicator that either the head-camera offset is wrong: https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Top-Camera-Setup#head-offsets or the bottom vision nozzle position is off (see below: Nozzle Center).

To benchmark if the head-camera offset is correct you can:
* place a PCB of your choice in the machine
* run fiducial check
* choose a component or pads that have an easy to spot center, press the "Position the camera at placement's location" button, verify the camera is pointing to the exact center
* press the "Position the tool at placement's location" button
* see if the nozzle tip is pointing exactly at the chip/pad center (as in view with the camera before), use magnifying glass if required.

There are other methods like using a piece of paper with a crosshair on it, using thin paper and making a hole into it with the nozzle tip or using rubber gum/clay and making a marking into it with the nozzle but from my experience these methods all require more preparations and tools than the above method.

Note that this head offset is only valid at a single z-height (your PCB height) and it is recommended to have all pick locations/feeders/strip tapes on the same z-height.

# Top Vision

## Units Per Pixel

If the top vision Units Per Pixel value is off it will result in:
* fiducial check either taking longer (as the machine does not move as far as it should and will require more moves to actually hit the fiducual) or it will fail the fiducial check altogether because it cannot center the fiducials in view. 
* strip feeder visual auto-setup not working or providing wrong results 
* visual camera jogging not moving the camera where the cursor is aiming 
* drag feeder vision

It will however have no influence on component placement accuracy.

# Bottom Vision

## Pipeline

<img src="https://user-images.githubusercontent.com/4028409/40357049-463f03f6-5dbb-11e8-8aa5-d36250614689.jpg" width="300">

Obviously the bottom vision lighting should create close to 100% reproduce-able conditions and the pipeline settings itself should be as error prone as possible so something like in the above image does not happen.

Details: https://github.com/openpnp/openpnp/wiki/CvPipeline

## Camera Orientation

The correct orientation for the bottom vision camera can be verified by moving the nozzle in view with the jog controls. This may sound obvious but pressing the jog-up button should move the nozzle upwards in view, pressing jog-left should move the nozzle left in view. As the bottom vision camera settings allow rotation and flipping the image on both axes this can quickly create a confusingly oriented image that will result in placement offsets going in the wrong directions.

## Nozzle Center

<img src="https://user-images.githubusercontent.com/4028409/40356160-9512c506-5db8-11e8-8e60-9b4b9cb29d8c.jpg" width="300">

Even with accurate end stops plus optical homing of the top vision camera we see that the bottom vision nozzle location can slightly vary with every homing run. Since the nozzle center being slightly off will result in all placements being slightly off it is highly recommended to verify and if necessary correct the bottom vision nozzle center location after every homing run.

## Units Per Pixel

If this value is not accurate it will result in bottom vision component offsets being under or overcompensated. Looking at vision debug images and resulting placement locations can give a clue here.

As it can be a challenge to find accurate rulers that can be picked up with the nozzle and held into bottom vision view we created this PCB for measuring Units Per Pixel (we tried squared paper but that keeps slightly bending):

<img src="https://644db4de3505c40a0444-327723bce298e3ff5813fb42baeefbaa.ssl.cf1.rackcdn.com/2ce0e7fee4dfe0856b4ccf0f5458cd87.png" width="250">

Download PCB or order from OSHPARK:
https://oshpark.com/shared_projects/DhjpjyLl

