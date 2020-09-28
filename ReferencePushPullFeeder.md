## What is it?

In short, the ReferencePushPullFeeder is a feeder where an elaborate motion can be actuated by an Actuator to advance the tape. The motion can be independent of the pick and it can perform a "hook and pull" articulation, therefore it can support 2mm pitch tapes (e.g. with 0402/0201 parts) where the tape is only advanced on every second feed. The feeder supports OCR part label recognition, optimized vision calibration, elaborate auto-learning, one-click auto-setup, OCR based feeder row discovery and more.

![Example Feeder in Action](https://user-images.githubusercontent.com/9963310/94424926-54387480-018b-11eb-96b1-963e69c14e50.gif)

## Features in more Detail

* Generic implementation for a feeder that can feed parts by performing push and pull (and other) motions using a head-mounted Actuator.
* Uses a semantic vision concept based on sprocket holes and knowledge of the EIA 481 standards.
* Employs a simple OCR vision stage that can identify the part from a label on the feeder.
* OCR based automatic feeder slot swapping, part changing or even new feeder creation with auto-cloned settings.
* Automatic and learning "only when needed" calibration for feeders that are mounted with large tolerances (+/-2mm).
* Any part pitch, any feed pitch, any tape width.
* Multi-feed: 2mm pitch (0402/0201) support i.e. the feeder will only advance the tape on every second feed. 
* Multiplier: For speed, you can use a Multi-feed for any pitch, i.e. the feeder can quickly actuate multiple times.
* Easy to clone & sync settings between feeders, by package or by (new) Tape&Reel Specification.
* One click setup from the second feeder.
* Handles geometric transformations for all the relevant settings (allows cloning settings from a “west” to a “south” feeder, for instance).

## Basic Setup

There is a video documenting the most important setup steps. You can watch this to get going and get a first impression. However, some steps aren't covered so you should come back to this page to fill in the missing details. Also note that the 3D-printed feeder featured in the video is just one example standing in for a wide variety of possible feeder hardware designs. Any feeder requiring any type of motion actuation (lever, knob, push, drag, etc. pp.) is supported. 

https://youtu.be/5QcJ2ziIJ14

### General Settings 

![General Settings](https://user-images.githubusercontent.com/9963310/94363615-af9f2f80-00c3-11eb-9688-5385838f3a6a.png)

The General Settings are as for every other feeder. However, because we will be using OCR to detect the right part, we just leave this on the default. 

### Locations

![Locations](https://user-images.githubusercontent.com/9963310/94363743-c09c7080-00c4-11eb-96ff-474db04ceb34.png)

The Locations define the orientation and location of the feeder _or rather of the tape_ in the feeder. Because it is much easier for computer vision to precisely see the sprocket holes than the parts in the tape's pockets, the sprocket holes will be used for precise tape location calibration. Given tape width and pitch settings, the ReferencePushPullFeeder can then use EIA 481 standardized tape geometry to deduce precise pick locations from the sprocket holes. 

Normally, you let computer vision get the locations automatically for you. When this is not possible e.g. for transparent tapes or when the camera can't reach the feeder, you can set the locations up using the standard capture buttons. Read the tooltips on the field labels to learn which location is which. 

For computer vision to work reliably, it is recommended to have a contrasting background behind the tape's sprocket holes. For best robustness use a vivid (i.e. high saturation) color, like the "green screen" that is used in movie special effects. The color-screening method works across a large range of brightnesses, even in difficult lighting/shadow situations. The ReferencePushPullFeeder comes with a default vision pipeline that is optimized for a green-yello-orange-red range of screening colors out of the box. However, the vision process is very flexible and also works with any custom sprocket hole detection method and many pipeline result types. So if your machine has a monochrome camera or non-color background, don't give up yet. In this case you need to edit the vision pipeline, see the [[Vision|ReferencePushPullFeeder#vision]] section below. The following assumes you have a working pipeline.

For a very quick setup, just move your camera center roughly over the pick location and press the Auto-Setup button. 
![Auto-Setup](https://user-images.githubusercontent.com/9963310/94364461-aadd7a00-00c9-11eb-908c-ae259b719d84.png)
If this is a 2mm pitch tape, choose the pick location closest to the tape reel, i.e. closest to where it is feeding from. You can also press the Preview Vision Features button to check first.  

![](https://user-images.githubusercontent.com/9963310/94364578-a36aa080-00ca-11eb-9ce5-a0160b7462c4.png)

The Auto-Setup will set up all your locations automatically. If you already have other ReferencePushPullFeeders defined, this will also clone some setting over from them (more about that later).  

**Important Note**: the Auto-Setup is really only a first-step tool. Do not use it once the feeder is already set up, hand-tuned settings may be overwritten! To avoid mishaps, a warning message box will ask you to confirm.

The **Normalize?** option will normalize the pick location(s) to nominal coordinates relative to the vision calibrated sprocket holes according to the EIA 481 standard. This means it does not matter how precisely you capture the Pick Location i.e. the center of the tape pocket in Auto-Setup (or manually). Your manual capture need only be within ±1mm of the true pick location. If you want to override the standard, switch **Normalize?** off.

The **Snap to Axis?** option will align the vision calibrated sprocket hole locations to the nearest axis, i.e. either to X or Y (within ±10°). This means you are trusting the mechanical squareness of your machine, feeder mount and tape guide more than any relative rotation of the sprocket holes obtained from computer vision.

### Tape Settings

![Tape Settings](https://user-images.githubusercontent.com/9963310/94364897-00ffec80-00cd-11eb-8d18-91e98d523656.png)

In the Tape Settings you can set the right **Part Pitch**. 

The **Feed Pitch** is the mechanical tape transport per feeder actuation. If the Part Pitch is larger than that, OpenPnP will automatically actuate the feeder multiple times. If the Part Pitch is less than that, OpenPnP will automatically skip actuation and instead iterate between the pick locations. 

The **Rotation in Tape** setting must be interpreted relative to the tape's orientation, regardless of how the feeder/tape is oriented on the machine. Unfortunately, it seems there is no universal industry standard of how to interpret the orientation of the tape. Furthermore, your E-CAD library parts might have mixed orientation. So let's proceed pragmatically as follows:

1. Look at the neutral upright orientation of the part/package as drawn inside your E-CAD library (orientation of pin 1/polarity/cathode etc.). This is 0° for the part.
2. Look at the tape with the sprocket holes on top. The direction of unreeling goes to the right and this is our 0° tape direction.
3. Determine how the part is rotated the tape, relative from its upright orientation (1). This is the **Rotation in Tape**.
 
The **Multiplier** allows you to actuate the feeder multiple times to feed more parts per serving, as a speed optimization. This may reduce the feed time per part because the actuator is already at the right place and/or engaged in the mechanics. 

### Vision

![Vision](https://user-images.githubusercontent.com/9963310/94425495-4e8f5e80-018c-11eb-92a0-0735bcaa81e6.png)

The **Calibration Trigger** determines when the feeder location is calibrated using computer vision. 

![Calibration Trigger](https://user-images.githubusercontent.com/9963310/94426384-c90cae00-018d-11eb-9052-9e5d74b60cc7.png)

* **None**: No calibration takes place. Use this setting for feeders/tapes, where sprocket hole recognition is impossible e.g. with a transparent tape or when the feeder is outside the camera reach.
* **OnFirstUse**: Calibration is performed on first use once after machine homing. You can press **Reset Statistics** to invalidate the current calibration. 
* **OnEachTapeFeed**: Calibration is performed on first use and after each tape feed. Use this setting when the tape transport is somewhat imprecise or if the parts are very small. 
* **UntilConfident**: Starts out like **OnEachTapeFeed**, but based on statistics over the accuracy of the tape transport, this will automatically skip further calibrations, once statistical confidence is reached. You can press **Reset Statistics** to invalidate the current calibration and recorded statistics. 

Calibration will also be invalidated whenever you change crucial feeder settings, such as Locations or when you press **Reset Feed Count**.



