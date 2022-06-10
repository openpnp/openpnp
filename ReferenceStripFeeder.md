# Setup

## Tape Strip Orientation

When setting the reference hole and second hole on ReferenceStripFeeder either manually or using Auto Setup, you have to choose your reference and second hole or part according to the standards used in SMT tape. Primarily this means that the reference hole should be the first hole 2mm from the first part in the direction of travel.

For ReferanceStripFeeder the ‘Direction of Travel’ and ‘Direction of Feed’ are confusing since nothing is being fed; the tape is fixed to a location.
 With the tape laid vertically if the holes are on the left of the part the Direction of Travel is from the top to the bottom, North to South. If the holes are on the right of the part the Direction of Travel is from the bottom to the top, South to North. 
 With the tape laid horizontally if the holes are on the bottom of the part the Direction of Travel is from the left to the right, West to East. If the holes are on the top of the part the Direction of Travel is from the right to the left, East to West. 
 The tape can be in any orientation on the table but the direction of travel along the tape always follows this rule. See the image below for clarification:

![strip feeder orientation](https://user-images.githubusercontent.com/3868450/34457250-6b69f62a-edab-11e7-8030-0eeed21a9692.png)

## CvPipeline

A CvPipeline can be customized to clean up the camera image for improved recognition. The pipeline can be configured by clicking Edit Pipeline in the feeders configuration.

The final pipeline stage (with detected circles) should have the name 'results' (plural, lower-case, no quotes). For detecting the feeding circles in the tape, DetectFixedCirclesHough should be used - if it isn't finding the correct circles either your camera's units-per-pixel is set incorrectly or you need to do further clean up of the camera image first.

During the feeder setup wizard, the strip feeder hole debug colors are as follows:

* Red are any holes that are found.
* Orange are holes that passed the distance check but failed the line check. **
* Blue are holes that passed the line check but are considered superfluous. **
* Green passed all checks and are considered good.

** - only displayed when Alt/Option is held down when the setup wizard is started.

## Z and Pick Height

When picking from the strip feeder, OpenPnP will lower the nozzle to the Z value specified on the first reference hole. The Z value for the second reference hole is ignored.

## Rotation in Tape
To pick up parts taking the right angle into account it has to be set in "Rotation in Tape". 

The **Rotation in Tape** setting must be interpreted relative to the tape's orientation, regardless of how the feeder/tape is oriented on the machine. Unfortunately, it seems there is no universal industry standard of how to interpret the orientation of parts or what is considered 0° inside the tape [(see here)](https://groups.google.com/g/openpnp/c/M93Ve67V-Xg/m/EpMJMLkFCAAJ). Furthermore, your E-CAD library parts might have legacy mixed orientations anyway. So let's proceed pragmatically as follows:

1. Look at the **neutral** upright orientation of the part package/footprint as drawn inside your E-CAD **library**.
1. ⚠ Double-check you are in the **library**, do **not** look at the part in the project PCB, this is _not_ neutral!  
1. See where pin 1 is, how the polarity, the cathode etc. are oriented. This is your 0° for the part. 

   ![library part](https://user-images.githubusercontent.com/9963310/173001959-d0b8e036-c73d-4e39-99ec-589f6b16d32c.png)

1. Look at the tape with the sprocket holes on the left. The direction of unreeling goes up and this is our 0° tape orientation here. 
1. Determine how the part is rotated inside the tape pocket, relative from its upright orientation in (1). This is the **Rotation in Tape**.

   ![Rotation in Tape](https://user-images.githubusercontent.com/9963310/173041573-f9a6ec7c-13ee-4ee9-b360-4b81bb90e54b.png)

1. Our example happens to have no rotation relative from its upright orientation in (1), so the correct **Rotation in Tape** is 0°. 


Note: having the holes on the left instead of the top is unfortunately different from the industry standard EIA-481-C orientation that is implemented for the [[BlindsFeeder]] and [[ReferencePushPullFeeder]].


## Video Tutorials

* Strip Feeder Auto Setup: https://www.youtube.com/watch?v=Fs-SwSq5AZw

# Troubleshooting

## "Unable to locate reference hole. End of strip?" 

This error usually occurs when the strip feeder vision system cannot find the next tape hole. The most common causes of this problem are incorrect units per pixel settings on the camera and poor lighting.

See https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-General-Camera-Setup#set-units-per-pixel to set and verify the units per pixel settings and make sure your camera is getting enough light. There should be strong contrast between the tape itself and the holes. The feeders CvPipeline can be adjusted to help address issues with your particular setup of camera and lighting, however in general it's best to start with a good hardware setup.

A good way to check that your units per pixel settings are correct is to right click the camera view and turn on the following options:
* Reticle -> Fiducial
* Reticle -> Options -> Units -> Millimeters
* Reticle -> Options -> Shape -> Circle
* Reticle -> Options -> Size -> 1.5

With these settings you should see a small circle appear in the camera view. This is a 1.5mm circle. If you line this circle up with a tape hole it should appear exactly the same size. If it doesn't, you need to adjust your units per pixels settings.