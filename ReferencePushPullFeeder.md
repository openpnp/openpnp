## What is it?

In short, the ReferencePushPullFeeder is a feeder where an elaborate motion can be used to advance the tape mechanically. The motion can be independent of the pick motion and it can perform a "hook and pull" articulation, therefore it can support 2mm pitch tapes (e.g. with 0402/0201 parts) where the tape is only advanced on every second feed. The feeder supports OCR part label recognition, optimized vision calibration, elaborate auto-learning, one-click auto-setup, OCR based feeder row discovery and more.

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

## Video

There is a video documenting the most important setup steps. You can watch this to get going and get a first impression. However, some steps aren't covered so you should come back to this page to fill in the missing details. Also note that the 3D-printed feeder featured in the video is just one example standing in for a wide variety of possible feeder hardware designs. Any feeder requiring any type of motion actuation (lever, knob, push, drag, etc. pp.) is supported. 

https://youtu.be/5QcJ2ziIJ14

## Reuse of Settings

The settings described in the following sections are numerous and it may sound like hard work to setup a single feeder. However most of these settings are fine to be left on the defaults, and the few exceptions must only be visited once, for the first feeder of a certain tape specification. The settings will subsequently be automatically cloned to the next feeder. See [[Clone Setting|ReferencePushPullFeeder#clone-settings]].

## General Settings 

![General Settings](https://user-images.githubusercontent.com/9963310/94363615-af9f2f80-00c3-11eb-9688-5385838f3a6a.png)

The General Settings are as for every other feeder. However, because we will be using OCR to detect the right part, we just leave it alone now. 

## Locations

![Locations](https://user-images.githubusercontent.com/9963310/94363743-c09c7080-00c4-11eb-96ff-474db04ceb34.png)

The Locations define the orientation and location of the feeder _or rather of the tape_ in the feeder. Because it is much easier for computer vision to precisely see the sprocket holes than the parts in the tape's pockets, the sprocket holes will be used for precise tape location calibration. Given tape width and pitch settings, the ReferencePushPullFeeder can then use EIA 481 standardized tape geometry to deduce precise pick locations from the sprocket holes. 

Normally, you let computer vision get the locations automatically for you. When this is not possible e.g. for transparent tapes or when the camera can't reach the feeder, you can set the locations up using the standard capture buttons. Read the tooltips on the field labels to learn which location is which. 

For computer vision to work reliably, it is recommended to have a contrasting background behind the tape's sprocket holes. For best robustness use a vivid (i.e. high saturation) color, like the "green screen" that is used in movie special effects. The color-screening method works across a large range of brightnesses, even in difficult lighting/shadow situations. The ReferencePushPullFeeder comes with a default vision pipeline that should work out of the box for a green-yello-orange-red range of screening colors. However, the vision process is very flexible and also works with any custom sprocket hole detection method and many pipeline result types. So if your machine has a monochrome camera or non-color background, don't give up yet. In this case you need to edit the vision pipeline, see the [[Vision|ReferencePushPullFeeder#vision--calibration]] section below. 

The following assumes you have a working pipeline. You can press the **Preview Vision Features** button to check the pipeline first.  

![](https://user-images.githubusercontent.com/9963310/94364578-a36aa080-00ca-11eb-9ce5-a0160b7462c4.png)

For a very quick setup, just move your camera center roughly over the pick location and press the Auto-Setup button. If this is a 2mm pitch tape, choose the pick location closest to the tape reel, i.e. closest to where it is feeding from.

![Auto-Setup](https://user-images.githubusercontent.com/9963310/94364461-aadd7a00-00c9-11eb-908c-ae259b719d84.png)

The Auto-Setup will set up all your locations automatically. If you already have other ReferencePushPullFeeders defined, this will also clone some setting over from them (more about that later).  

**Important Note**: the Auto-Setup is really only a first-step tool. Do not use it once the feeder is already set up, hand-tuned settings may be overwritten! To avoid mishaps, a warning message box will ask you to confirm.

The **Normalize?** option will normalize the pick location(s) to nominal coordinates relative to the vision calibrated sprocket holes according to the EIA 481 standard. This means it does not matter how precisely you capture the Pick Location i.e. the center of the tape pocket in Auto-Setup (or manually). Your manual capture need only be within ±1mm of the true pick location. If you want to override the standard, switch **Normalize?** off.

The **Snap to Axis?** option will align the vision calibrated sprocket hole locations to the nearest axis, i.e. either to X or Y (within ±10°). This means you are trusting the mechanical squareness of your machine, feeder mount and tape guide more than any relative rotation of the sprocket holes obtained from computer vision.

## Tape Settings

![Tape Settings](https://user-images.githubusercontent.com/9963310/94364897-00ffec80-00cd-11eb-8d18-91e98d523656.png)

In the Tape Settings you can set the right **Part Pitch**. 

The **Feed Pitch** is the mechanical tape transport per feeder actuation. If the Part Pitch is larger than that, OpenPnP will automatically actuate the feeder multiple times. If the Part Pitch is less than that, OpenPnP will automatically skip actuation and instead iterate between the pick locations. 

The **Rotation in Tape** setting must be interpreted relative to the tape's orientation, regardless of how the feeder/tape is oriented on the machine. Unfortunately, it seems there is no universal industry standard of how to interpret the orientation of the tape. Furthermore, your E-CAD library parts might have mixed orientation. So let's proceed pragmatically as follows:

1. Look at the neutral upright orientation of the part/package as drawn inside your E-CAD library. Look for the orientation of pin 1, polarity, cathode etc. This is 0° for the part.
2. Look at the tape with the sprocket holes on top. The direction of unreeling goes to the right and this is our 0° tape direction.
3. Determine how the part is rotated the tape, relative from its upright orientation (1). This is the **Rotation in Tape**.
 
The **Multiplier** allows you to actuate the feeder multiple times to feed more parts per serving, as a speed optimization. This may reduce the feed time per part because the actuator is already at the right place and/or engaged in the mechanics. 

## Vision

### Vision / Calibration

![Vision Calibration](https://user-images.githubusercontent.com/9963310/94429454-97e2ac80-0192-11eb-9121-1634f343fb44.png)

The **Calibration Trigger** determines when the feeder location is calibrated using computer vision. 

![Calibration Trigger](https://user-images.githubusercontent.com/9963310/94426384-c90cae00-018d-11eb-9052-9e5d74b60cc7.png)

* **None**: No calibration takes place. Use this setting for feeders/tapes, where sprocket hole recognition is impossible e.g. with a transparent tape or when the feeder is outside the camera reach.
* **OnFirstUse**: Calibration is performed on first use once after machine homing. You can press **Reset Statistics** to invalidate the current calibration. 
* **OnEachTapeFeed**: Calibration is performed on first use and after each tape feed. Use this setting when the tape transport is somewhat imprecise or if the parts are very small. 
* **UntilConfident**: Starts out like **OnEachTapeFeed**, but based on statistics over the accuracy of the tape transport, this will automatically skip further calibrations, once statistical confidence is reached. You can press **Reset Statistics** to invalidate the current calibration and recorded statistics. 

Calibration will also be invalidated whenever you change crucial feeder settings, such as Locations or when you press **Reset Feed Count**.

The **Precision wanted** is the tolerance in pick location that can be tolerated for this feeder/part. Only used with the **UntilConfident** setting. This should be no larger than about 1/4 of your part's contact size or pitch. 

The **Precision Average**, **Precision Confidence Limit** and **Calibration Count** are the statistical indicators showing the precision of your feeder's tape transport, i.e. the repeatability of the pick locations deduced from the sprocket holes after tape transport. You can press **Reset Statistics** to start a fresh statistics e.g. after re-mounting a feeder or after changing feeder settings that might impact precision. 

### Vision / OCR

![grafik](https://user-images.githubusercontent.com/9963310/94429541-bfd21000-0192-11eb-9f29-2770279c87c6.png)

The **OCR Wrong Part Action** determines what should happen if the OCR detects a different part than set in the General Settings: 

![grafik](https://user-images.githubusercontent.com/9963310/94429376-80a3bf00-0192-11eb-9df1-4ae0362a9aab.png)

* **None**: Use this setting if you don't want to use OCR. 
* **SwapFeeders**: If a wrong part is detected but the right part is selected in a different ReferencePushPullFeeder, the locations of the two feeders are swapped. The swapped-in feeder will be enabled. This will happen, if you unload/reload/rearrange your feeders on the machine.
* **SwapOrCreate**: Works like **SwapFeeders**, but if no other feeder with the right part is found, a new one will be created and swapped-in at the current feeder's location. The current feeder is then disabled in turn (they are now sitting at the same location and only one must be enabled). 
* **ChangePart**: The part in the current feeder is changed. This will only work correctly, if the tape settings etc. remain the same between the parts i.e. if you restrict any reloading/rearranging to groups of feeders with the same settings. 
* **ChangePartAndClone**: The part in the current feeder is changed but settings are cloned from a template feeder (see [[Clone Setting|ReferencePushPullFeeder#clone-settings]]). 

The **Stop after wrong part?** option will stop any process (e.g. pause the Job) after a wrong part was detected and the chosen action performed. The user can then review the changes before resuming. 

The **Check on Job Start?** option will perform calibration and OCR detection on all enabled ReferencePushPullFeeders, before the Job is started so any changed parts can be resolved up front. A Travelling Salesman optimization will be used to visit all the feeders along an optimal path. 

The **OCR Font Name** and **OCR Font Size [pt]** settings determine the font used to print the label on the feeder. Make sure to have the font installed on the machine that runs OpenPnP. NOTE: the currently selected font will appear in the selection box regardless of wheter it is installed on the system or not (it will then appear at the top). If in doubt, use a different application to make sure. Users have reported that under Windows fonts need to be installed **system-wide** rather than just for the logged-in user in order to work in Java/OpenPnP. 

The **Setup OCR Region** button can be used to define the area of the Camera View that is taken for OCR. The instructions displayed underneath the camera view will guide you through the steps. You will be asked to define the region by clicking on the corners of a rectangle. If the label is rotated, make sure to select the corners in the sense of the text orientation. Also make sure to allow for enough space around the characters, i.e. the whole character box must be within the region for a character to be successfully detected. 

![OCR Region](https://user-images.githubusercontent.com/9963310/94434266-c912ab00-0199-11eb-8947-dcb5cd0f71d0.png)

Use the **Part by OCR** button to perform the OCR action manually. 

Use the **All Feeder OCR** button to perform the OCR action on all the feeders manually. Again a Travelling Salesman optimization will be used to visit all the feeders along an optimal path. 

**NOTE**: the feeder will automatically and dynamically generate the minimal **OCR alphabet** from all the part IDs definied in OpenPnP. Characters not occuring anywhere in part IDs will not be recognized. Therefore you must make sure that parts are imported from the E-CAD (or manually defined) before testing OCR. 

### Vision / Edit Pipeline

As usual, use the **Edit Pipeline** and **Reset Pipeline** buttons. 

![Buttons](https://user-images.githubusercontent.com/9963310/94434817-9fa64f00-019a-11eb-9095-392c0b95da2e.png) 

**NOTE**: when editing the Pipeline, be aware that many stage properties are controlled by the feeder i.e. the ones visible in the stages will be ineffective. The AffineWarp is fully controlled by the OCR region set up in the feeder. Likewise the OCR Font etc. are controlled by the corresponding feeder settings, the OCR Alphabet is generated dynamically. 

![Editing Pipeline](https://user-images.githubusercontent.com/9963310/79022084-7870ac80-7b7d-11ea-9d7e-83efb3004551.png)

## Clone Settings

![Clone Settings](https://user-images.githubusercontent.com/9963310/94479451-1bbd8880-01d5-11eb-9f1a-a2f5488e72ea.png)

Setting up a ReferencePushPullFeeder may involve many settings and it is clear that we want to re-use these settings comfortably. We want OpenPnP to clone settings from one feeder definition to the next, either automatically, when creating a new feeder, or when we specifically want to revise the settings of a whole group of feeders. 

Not all feeders/tapes are the same, therefore OpenPnP need to know how to treat them as a group with common settings. By default the group is defined by the **packages** of the parts that are selected in the feeders. To further group similar tapes/parts, you can set a **Tape Specification** on the Packages Tab:

![Tape Specification](https://user-images.githubusercontent.com/9963310/94481324-d3539a00-01d7-11eb-9d5a-b83d28ddf577.png)

This is just a textual tag, grouping packages with the same Tape Specification. The example above just uses a common way to describe the relevant properties of the tapes, like for example `width x pitch`. The pattern is completely up to the user. 

Once we have defined the grouping, we must designate one feeder in each group as the template, where all the settings are managed. This is done using the **Use this one as Template?** option. 

The **Template** info will show you which feeder clones to which either actively...

![grafik](https://user-images.githubusercontent.com/9963310/94482242-44478180-01d9-11eb-9c87-420df8e3faa2.png)

... or passively...

![grafik](https://user-images.githubusercontent.com/9963310/94482353-6a6d2180-01d9-11eb-9638-657a4ff12ecf.png)

If you want to change the settings of a whole group of feeders, you can edit the template and then use the **Clone to Feeders** button. The various option checkboxes will then determine which settings are cloned over. 

![Clone to Feeders](https://user-images.githubusercontent.com/9963310/94482663-f0896800-01d9-11eb-852c-73b10873dc64.png)

(work in progress)

