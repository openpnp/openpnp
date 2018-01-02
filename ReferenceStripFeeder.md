# Setup

## Tape Strip Orientation

When setting the reference hole and second hole on ReferenceStripFeeder either manually or using Auto Setup, you have to choose your reference and second hole or part according to the standards used in SMT tape. Primarily this means that the reference hole should be the first hole 2mm from the first part in the direction of travel.

The direction of travel is fixed, and is always north to south with the holes on the left. The tape can be in any orientation on the table but the direction of travel along the tape always follows this rule. See the image below for clarification:

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
To pick up parts taking the right angle into account it has to be set in "Rotation in Tape". The part's orientation is referenced to the direction of feed (direction of unreeling) and thus independent of the tape's orientation on the table. See the image below for some clarifying examples: 
![rotation in tape_examples](https://user-images.githubusercontent.com/3868450/34457251-6b83c38e-edab-11e7-8e5a-f6e4212db124.png)

For a summarizing article on the orientation of parts according to common standards see https://blogs.mentor.com/tom-hausherr/blog/tag/ipc-standards/

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