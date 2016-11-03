# Tape Strip Orientation

When setting the reference hole and second hole on ReferenceStripFeeder either manually or using Auto Setup, you have to choose your reference and second hole or part according to the standards used in SMT tape. Primarily this means that the reference hole should be the first hole 2mm from the first part in the direction of travel.

The direction of travel is fixed, and is always north to south with the holes on the left. The tape can be in any orientation on the table but the direction of travel along the tape always follows this rule. See the image below for clairification:

![strip feeder orientation](https://cloud.githubusercontent.com/assets/1182323/12517177/6108c344-c0e6-11e5-9228-874a35a3fa5c.png)

# Video Tutorials

* Strip Feeder Auto Setup: https://www.youtube.com/watch?v=Fs-SwSq5AZw

# Troubleshooting

## "Unable to locate reference hole. End of strip?" 

This error usually occurs when the strip feeder vision system cannot find the next tape hole. The most common causes of this problem are incorrect units per pixel settings on the camera and poor lighting.

See https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-General-Camera-Setup#set-units-per-pixel to set and verify the units per pixel settings and make sure your camera is getting enough light. There should be strong contrast between the tape itself and the holes.

A good way to check that your units per pixel settings are correct is to right click the camera view and turn on the following options:
* Reticle -> Fiducial
* Reticle -> Options -> Units -> Millimeters
* Reticle -> Options -> Shape -> Circle
* Reticle -> Options -> Size -> 1.5

With these settings you should see a small circle appear in the camera view. This is a 1.5mm circle. If you line this circle up with a tape hole it should appear exactly the same size. If it doesn't, you need to adjust your units per pixels settings.