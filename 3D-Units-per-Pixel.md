# What is it?
As you know, the closer an object is to a camera, the larger it appears in the camera view. When you need to measure or detect certain shapes, sizes or distances, you need to know at what distance the subject is. On a Pick and Place machine, the first and foremost method of ensuring this, is to make sure, all such subjects (the Homing Fidcuial, PCBs, feeders, nozzle tip changers etc.) are all at the same distance from the camera. This means, they are all flush with one universal machine table surface level. _All at the same uniform Z coordinate._

**Note: This should still be a very important design goal for any machine. The calibration we are discussing here should only be a second-level fall-back. Achievable precision will still be much better with uniform Z.**

However, this is not always possible, especially in a DIY environment. Some feeders may be mounted on top of the table etc. Enter **OpenPnP 3D Units per Pixel** (brought to OpenPnP by Tony Luken). These are used to determine the camera viewing scale at different distances, i.e. at different Z coordinates. Whenever the Z coordinate is known (such as the pick location Z of a feeder), we can now automatically adjust the viewing scale, called **Units per Pixel**, to the Z. 

# Calibrating and Enabling 3D Units per Pixel

Camera 3D Units per Pixel are automatically calibrated, when you use [Vision Solutions](https://github.com/openpnp/openpnp/wiki/Vision-Solutions) from [[Issues and Solutions]]. 

Best [watch the video](https://youtu.be/md68n_J7uto).

Once all the steps have been completed, the camera shows an enabled **3D Calibration**:

![3D Units per Pixel](https://user-images.githubusercontent.com/9963310/131219497-938464d2-697e-4ec2-90c0-dc706fbca421.png)

**Units per Pixel** is just a fancy name for "how large is one pixel in real world units". The numbers in the screen-shot (above) simply mean that one pixel covers approximately 0.03213mm x 0.03216mm in the real world.

The **Default Working Plane** is a very, very important Z coordinate for your machine. It sets that "universal table surface Z" we were talking about in the intro. Your homing fiducial, your primary calibration fiducial **must absolutely** be on that same Z level. Your PCB surface level and the focal planes of both your cameras should really, really too. Other things as far as possible. 

Manual 3D Calibration is also possible, follow the instructions on the camera Wizard:

![Manual Calibration](https://user-images.githubusercontent.com/9963310/131219525-911702a1-7275-432f-86c2-f5468a69bc1a.png)

# Camera Z Axis sets the Viewing Plane

For the 3D Units per Pixel to be effective, the camera must have a [virtual Z axis](https://github.com/openpnp/openpnp/wiki/Machine-Axes#referencevirtualaxis) assigned (see [[Mapping Axes]]). Whenever the camera moves to a location to perform a vision operation (e.g. over a pick location on a feeder), it will physically move in X, Y but also virtually in Z. By thus moving the Z, it will implicitly set its Units per Pixel to the right scale. 

The viewing plane Z is now visible in the Camera View lower right corner, and if you have the Ruler reticle activated, you will also see how the ticks on the ruler change according to scale (select the camera in the Machine Controls and jog Z up and down): 

![Camera View 3D Z](https://user-images.githubusercontent.com/9963310/131219680-fa0a4dc5-9800-49a8-ac0d-119e098e6c5c.png)

Note: although this was not tested, it should theoretically work the same way for a camera that has its own _real/physical_ Z axis, that can move up and down to focus subjects. The Units per Pixel would not change in this scenario, but the camera viewing or focal plane would. Because the camera still has a distance from its focal plane, moving the camera down in Z is still safe. The 1:1 working principle shows that using the Z axis to "focus" on a subject is quite a natural thing to do.

# Using with Feeders

All the OpenPnP feeders using Computer Vision should now work in 3D (if not, please report a bug :-)). When scale is important, e.g. to recognize carrier tape sprocket holes by their size and pitch, the relevant Z must be known, for the feeder vision to work. Therefore, you might see an error if this Z coordinate is not yet set:

![Z missing Error](https://user-images.githubusercontent.com/9963310/131219923-afa8f871-2773-4f16-9532-67028acb96b4.png)

You might have to change your routine when setting up new feeders, always probe and enter the Z first. For simple feeders, you can probably just copy the Z coordinate over (you'll know it by heart, after a while). BlindsFeeder and ReferencePushPullFeeder even automatically propose the Z from already present _array_ or _template_ feeders, respectively. 

Note that for some feeder types, pipeline editing requires Z too (because the pipeline needs Units per Pixel to work correctly). The same error dialog will appear if Z is missing.

## ReferenceStripFeeder

The following shows an Auto-Setup of a [[ReferenceStripFeeder]] at higher than usual Z (it was deliberately proped up by ~7mm). Normal setup (without 3D Units per Pixel) failed. With the 3D Calibration it works! Observe how after the Auto-Setup the ruler ticks align with the 2mm part pockets quite nicely: 

![3d-calibration-stripfeeder](https://user-images.githubusercontent.com/9963310/131221108-0e535cd7-6ba7-4b18-af66-693391828aea.gif)

## BlindsFeeder

Similar, a [[BlindsFeeder]] doing cover edge calibration (cover open close). This is now bang-on, the iteration always overcompensated before:

![3d-calibration-blindsfeeder](https://user-images.githubusercontent.com/9963310/131221121-3668174b-0b3b-4d75-bdf6-62e71c56d24e.gif)

## ReferencePushPullFeeder

Same for the [[ReferencePushPullFeeder]] doing sprocket hole and OCR recognition, including scaling the OCR Region of interest:

![3d-calibration-pushpullfeeder](https://user-images.githubusercontent.com/9963310/131221124-95220bd8-6ac7-4f5a-8ab7-bf3c4e45fc03.gif)

## Other Feeder Types

Other feeder types with vision have also been reworked to support the 3D operation, albeit without testing. Please report any issues you might observe. 

# Nozzle Tip Changer

The [[Nozzle Tip Changer]] vision will now also use 3D Calibration. Because the locations that are used to define the template shot have arbitrary Z coordinates (the nozzle tips are diving below the structure of the changer), there is a new **Adjust Z** field to offset the Z: 

![Nozzle Tip Changer](https://user-images.githubusercontent.com/9963310/131222039-5385d94e-b10a-4bda-9716-5b84617996f2.png)


# Other Uses 

* PCB fiducials will be correctly detected and more efficiently closed in on, according to the PCB Z. Please note that having your PCB at a Z other than your **Default Working Plane** is still **not recommended!** 
* [[Visual Homing]] is assumed (by definition) to be at **Default Working Plane**, as set on the Camera. You must make sure that the visual homing fiducial and your [primary calibration fiducial](https://github.com/openpnp/openpnp/wiki/Vision-Solutions) are both at the same Z coordinate i.e. what will be **Default Working Plane**. These are the very foundations of your machine calibration, so there is deliberately no flexibility there!