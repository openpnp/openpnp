This file lists major or notable changes to OpenPnP in chronological order. This is not
a complete change list, only those that may directly interest or affect users.

# 2020-06-23

## Actuator API Change (Non-Breaking)

- Actuator.actuate(String) has been added, along with support in GcodeDriver. This makes it
  easy to send completely custom commands from machine object implementations. This is an
  optional, non-breaking API change.

# 2020-05-18

## Camera Jogging Revert

- The old method of camera jogging by clicking and dragging anywhere has been restored. This
  was removed when camera rotation jogging was adding because it seemed necessary with the
  new drag handles, but it turns out it's not necessary and the new version was far less
  convenient.

# 2020-05-17

## Sponsors and About Dialog

- The About dialog now includes a Credits tab to thank sponsors of the project. A SPONSORS.md
  file is also included, which is shown in the dialog.  

- The About menu item is now correctly added to the Help menu on MacOS when the MacOS integrations
  are not available.
  
## Installer Improvements

- Sample files are now installed by the installer in the user's Documents/OpenPnP directory,
  instead of in the installation directory. This fixes an issue #836 where users loading the
  sample files on Windows would experience a write error.

- Install4J has been updated from version 6 to 8. 
  

# 2020-04-29

## Advanced Camera Settle

Camera Autosettle is now exposed on the GUI, it was significantly expanded to allow for
different methods in image processing and for settling quantification. Graphical diagnostics 
and image replay with motion "heat map" are provided. 

## Advanced Part On/Off Detection

Vacuum sensing part-on/part-off detection was significantly expanded to allow for differencial
vacuum level assessment and adaptive dwell times. Graphical diagnostics are provided.

# 2020-04-20

## Improved Actuator Read Errors

Actuator reads are used for a number of subsystems in OpenPnP, and this feature tends to be
confusing to configure. Previously, a misconfigured actuator read would often result in
a cryptic NullPointerException that was difficult to debug. This system has now been improved
so that the Actuator will report the three most common errors: missing command, missing regex,
and unmatched response.

Additionally, actuatorRead() will no longer return null under any circumstance. It will either
return a valid string (which may be empty) or throw a descriptive error.

## GcodeDriver Test Framework

A new GcodeServer class has been added, along with a number of small GcodeDriver based tests.
The GcodeServer in combination with the test system allows end to end automated testing of complex
features such as Actuators. 

# 2020-04-12

## OpenCV Upgrade

OpenCV has been upgraded to 4.3.0-0. This is a major update (for OpenCV) and may introduce new
issues. This is a necessary step on the way to Java 10+ support.

Note that support for 32 bit Linux has been dropped from the OpenCV package due to difficulty
of maintenance. If you are using 32 bit Linux please make it known.


# 2020-04-02

## Vacuum and Blow Off Levels

You can now specify vacuum and blow off levels on packages. These values are used to trigger
actuators when picking (vacuum level) and placing (blow off level).

See https://github.com/openpnp/openpnp/pull/965 for more information.

Thanks Thomas!


# 2020-01-22

## Camera Jogging Update

A new camera jogging system has been implemented which allows for rotation and better precision
for XY. See https://www.youtube.com/watch?v=0TvqQBkTGP8 for details.

# 2019-12-03
	
## New feeder: ReferenceLeverFeeder

This is for feeders where a lever is pushed by a actuator (usually the head) to advance the parts.  It was made from the ReferenceDragFeeder.  A Part Pitch field is added and multiple feed operations will be performed as required.

# 2019-11-24

## BREAKING CHANGE: Vacuum Valve and Pump Actuation Remodel

Vacuum valves and pump actuation is now modeled with Actuators and  
made independent from pick() and place() functionality and from the Driver. The changes
refine and unify the machine model and ease the development of new capabilities. The new 
ContactProbeNozzle demonstrates this by adding contact probing to pick() and place() 
which allows for more tolerance and robustness in the Z height handling of parts, feeders 
and the PCB. 
* The vacuum Actuator is no longer just used to read the vacuum pressure level but also to
  actuate the valve (Boolean). 
* The Nozzle's pick() and place() methods will actuate the valve Actuators as needed.
* At the same time the Nozzle's pick() and place() methods are now dedicated to what their 
  names imply, making vacuum switching independent from them.
* Consequently the Nozzle pick() or place() can now do more than just actuate the vacuum, 
  they can also be properly sub-classed/overridden for extended functionality. 
* In turn the valve Actuator is now used independently to test if a part if off after 
  placement. An "in-the-air" pick() and place() cycle  is no longer used, avoiding 
  unexpected behavior when pick() and place() do more than vacuum switching.
* Users can also use the Machine Controls' Actuators tab to test the valve Actuator 
  independently and to simulate and determine part on/part off vacuum readings. 
* A new pump Actuator is added to the head. It will actuate the pump as needed.
* The pick() and place() methods in turn govern the pump Actuator, i.e. as long as at 
  least one part is on a nozzle, the pump will be switched on.
* Users can also use the Machine Controls Actuators tab to test the pump Actuator.
* All pick(), place(), pump and valve actuation are removed from the Driver.
* Consequently the PICK_COMMAND, PLACE_COMMAND, PUMP_ON_COMMAND, PUMP_OFF_COMMAND are 
  deprecated in the GCodeDriver. They will no longer work but the fragments are available
  in the GUI for G-Code migration to the Actuators (these entries will be removed end of 
  2021).
* In turn the new Actuators define their G-Code in the proper ACTUATE_BOOLEAN_COMMAND 
  fragments. 
* Users must add the new pump Actuator and revisit the vacuum reading Actuator and 
  migrate their G-Code there. 
* To ease the now more important plug-and-play of Actuators, they can now be assigned by 
  ComboBoxes rather than by loose name references. 
* As a first benefit from the changes, the new ContactProbeNozzle subclasses the 
  ReferenceNozzle and adds a contact probe to sense when the nozzle tip hits the target
  i.e. when the pick() makes contact with the part in the feeder or when the place() hits 
  the PCB with the part on the nozzle (the Liteplacer has such a probing nozzle). This 
  allows for more tolerance and robustness towards variances in Z height of parts, 
  feeders and the PCB. Users can more quickly (i.e. more roughly) setup Z heights.
* More information, guidance and screenshots can be seen in the PR:
  https://github.com/openpnp/openpnp/pull/859#issue-290920991

## ReferenceStripFeeder Vision Bug Fix

A long standing bug in ReferenceStripFeeder has been fixed by @tjanuszewski in
https://github.com/openpnp/openpnp/pull/919. If you have struggled with the vision
on ReferenceStripFeeder in the past, especially on longer strips, please give it a new
try as this fix seems to improve it's functionality greatly.

Thank you @tjanuszewski for finding and fixing this!


# 2019-06-13

## New Scripting Events

Added several new scripting events for pick and place events:

* Nozzle.BeforePick
* Nozzle.AfterPick
* Nozzle.BeforePlace
* Nozzle.AfterPlace

# 2019-06-12

## Switcher Camera and Camera Interface Changes

This update adds a new camera type called a SwitcherCamera. This camera is a virtual camera
that allows you to have multiple virtual cameras sharing the same physical capture device. This
is a common configuration on commercial desktop pick and place machines, where a single capture
card captures images from two analog cameras. A serial command is used to switch which camera
is currently streaming.

Some small changes were also made to the camera interface in general. Two new methods were added:
* Camera.captureForPreview(): Captures an image and applies transforms, but does not perform
  scripting or lighting events.
* Camera.captureRaw(): Returns a raw image from the capture device, with no transforms.

The first is mainly for future expansion - in the near future changes will be made to how camera
streaming works to improve performance and make the cameras more context sensitive.

The second is added specifically for the new SwitcherCamera, so that it can get raw images from
the source camera. This new method is also being used to clean up and consolidate the camera
code across various camera implementations.

In general, if you aren't using the SwitcherCamera you shouldn't notice any differences with
this update. If you notice new problems with cameras, please report an issue.

See https://github.com/openpnp/openpnp/wiki/SwitcherCamera for more information.


# 2019-06-10

## Global Nozzle Tip Update

The nozzle tip system has been overhauled so that nozzle tips belong to the machine, rather than
to each nozzle. This removes the need to duplicate nozzle tips for each nozzle, and better
fits how nozzle changers typically work.

Additionally, you can now easily set package compatibility directly from the
packages panel.

This is a large, breaking change. For more information on why this change happened, please see:
https://github.com/openpnp/openpnp/issues/183

Thank you to @markmaker for reviewing and for merging in his recent calibration changes. He has
also provided some help for migrating:

- copy the big `<nozzle-tips/>` XML block to the right place (I took a default OpenPNP 2.0 `machine.xml` as a guide). 
- start OpenPNP again and again, note the elements/attributes no longer supported and delete them (would be so nice to have a command-line option for the XML parser to ignore unknown elements and attributes, but it seems the parser in OpenPNP has no such thing*)
- define nozzle tip to nozzle compatibility on the Nozzle
- learn the new way to change nozzle tips (by clicking the checkbox in the list)
- define vacuum levels not forgetting the fact that isPartOff is now measured with opened valve for a moment (but see #855).


# 2019-06-02

## Runout Compensation and Bottom Camera Position and Rotation Calibration

Feature changes:
* Using the Runout calibration facility, an automatic bottom camera position and rotation offset 
  calibration is provided. In a multi-nozzle machine, this should be done with the first/best nozzle.
* For all the other nozzles in a multi-nozzle machine, a new "Model & Camera Offset" calibration system is
  provided, compensating for any offset introduced through imperfect Z travel when the bottom camera focal 
  plane does not match the PCB surface plane. 
* The user can not set how many missed vision detections are tolerated in Runout Calibration for more 
  robustness inside a job.
* The user can set a Z offset for calibrate per nozzle tip to allow the focal plane of the vision-detected 
  feature to be further up on the nozzle tip (e.g. receded air hole in a cup shaped nozzle tip).
* If a nozzle tip is named "unloaded" it is used as a stand-in when no nozzle tip is loaded. 
  Very useful when the nozzle tip holder itself has so much runout that even nozzle tip changing 
  is scary without calibration. Again use the Z offset to calibrate the bare nozzle tip holder in the 
  focal plane. 
* User settable automatic recalibration trigger:
   - On each nozzle tip change
   - On each nozzle tip change if used in a job (like before)
   - On homing/first change of a nozzle tip after homing. Nozzle tips that are later unloaded 
     and reloaded will not be recalibrated, saving time. This assumes that both the nozzle (i.e. 
     the C axis) and the nozzle tip in the holder will retain a known rotation (i.e. the runout 
     phase shift does not change).
   - On manual calibration only. The compensation is stored in the machine.xml which is useful 
     for machines that have a homed/spring-loaded rotation C axes so the runout phase shift 
     remains the same on every power up.

Fixes:
* Removed unnecessary start/stop rotations in calibration
* Changed the threshold property into a unit-aware Length
* Fixed missing LengthUnit conversions in the  "Model" calibration system (it compensated in 
  millimeters but based its model on camera units).
* Fixed camera not updating its width/height when the frame size changes due to changed transformations 
  such as rotation (like after the above-mentioned rotation calibration). VisionUtils performed
  bad pixel to Location coordinate conversions until the next OpenPNP restart.

Changes in models and utility functions:
* Added a home() method to all HeadMountables to trigger recalibration when needed.
* Added a Camera.getLocation(tool) method to get a camera position adjusted for a specific 
  nozzle/nozzle tip. This helps with multi-nozzle machines that have the bottom camera
  focal plane at a Z height that differs from the PCB surface. Therefore the different nozzles might
  introduce a slight offset due to the Z axes not being perfectly perallel to each other. 
* Added reverse transformation in VisionUtils (Location to pixel)
* Added "center" property to vision stage MaskCircle to allow off-center nozzle tip recognition
  for camera calibration.
  
For more information see https://github.com/openpnp/openpnp/pull/825
  
# 2019-06-01

## OpenPnP 2.0 Ongoing Changes

* Actuators are now added to the tools dropdown in jog controls so that you can select one and
  move it. This is probably a temporary change as this dropdown will go away in the future, but
  for the time being it helps with setting up drag feeders.
  
* Swapped positions of "capture" and "move" buttons wherever those buttons are grouped together,
  and added a separator between the two groups of buttons. This is intended to make it a little
  less easy to accidentally click the capture buttons, which many people have recognized as being
  a UX problem.

* Moved the "Feed" button on the Feeders panel to the front of the list as this is the most
  common action.

* Split the reference nozzle and reference nozzle tip configuration wizards into separate panels.
  This is will make it easier to refactor for global nozzles.
  
* Added basic Z Probing by setting actuator name on head.

* Added canned cycle for getting a Z Probe at a location.

* Z Probe is now automatically performed, if enabled, when capturing a camera location. This
  results in a fully formed location capture including Z. If no Z Probe is available Z is left
  unchanged. 

# 2019-05-27

## OpenPnP 2.0

This update includes a large number of major changes to how OpenPnP works, along with several
breaking changes. Because of the severity of the changes I am calling this OpenPnP 2.0.

Please read the release notes carefully before using it, and please back up your configuration
directory, job, and board files. Some of the changes will modify your files and you will not be
able to go back to the older version without restoring your backups.

* JobProcessor Rewrite: The JobProcessor has been rewritten from the ground up to solve a number
  of long standing bugs and issues. The new version allows for finer granularity in steps, better
  error handling - including context sensitive errors, better retry options for pick and feed
  failures, improved vacuum checking, and increased flexibility.
  
  * The FSM implementation that caused the "No defined state from..." type errors has been removed
    completely. Fixes #695.
    
  * Each step is now in it's own class, and maintains it's own state. This lets each step determine
    what level of granularity it wants to expose.
     
  * Steps now direct the job processor to the next step when they finish, making it very easy to
    add customized steps and to retry when things fail.
    
  * The JobProcessor interface now returns a custom exception type that includes a source. In
    general these sources will be instances of model or SPI objects such as Nozzle, Feeder, Part, 
    Placement, etc. This makes it possible to help the user find the source of the error in the UI.
    
    A future update will include a new "Production" panel that will let you quickly jump to
    the source of an error.
    
  * Granularity has been greatly improved in several long running steps. Fiducials, and pick, 
    primarily. Now clicking stop during a fiducial check will not continue checking every board
    before stopping.  
    
  * Vacuum checking is now performed in the job processor, instead of in the nozzle. With this
    change additional vacuum checks have been added as well. Vacuum is now checked after pick,
    after bottom vision, before place, and after place. In addition, the after pick check is
    now performed after lifting the nozzle for better accuracy and the after place check is
    performed with vacuum turned on.
    
  * Feed retry and pick retry are now separate steps.
  
    Feed retry allows retry of a feeder that has an error while feeding, such as a vision failure
    on the strip feeder, or a hardware error on an auto feeder.
    
    Pick retry handles retry of the entire pick sequence. This is primarily a factor when using
    vacuum sensing. After a part is picked, if the vacuum sensing system does not detect the part
    on the nozzle a discard cycle is performed and the entire feed / pick cycle is restarted.
    
  * These changes fix #280.
    
* BREAKING CHANGE: Changed the vacuum sensing levels from the trio of logic inverted, 
  part on level, and part off level to a dual range for on low/high and off low/high.

* BREAKING CHANGE: Removed job auto save and config auto safe from the job processor.
  This feature caused a serious performance hit and cluttered up the job processor.
  This feature will be added back at a later date with a new implementation that does not harm
  performance.
  
* BREAKING CHANGE: Removed "Check Fids?" from the placements panel, and it's functionality from the
  job processor. This feature was originally meant to be a way to check local fids around
  important placements but it was not implemented correctly and was not generally useful.
  PRs welcome to add true local fid checking.

  This change should not break any jobs or configurations, but I've labeled it a breaking change
  to get the attention of anyone who was using the feature for further discussion.

* BREAKING CHANGE: The park when complete option has been removed and park when complete is now
  always performed. **You must set a park location before running a job on this version.** 
  This is a preperatory change for more automation in machine movements in general.
  It will become much more important that the machine knows where it can safely park.

* BREAKING CHANGE: Removed the isPartOn and isPartOff variables from the GcodeDriver. This is
  better handled by the main vacuum system. It would be possible to add the new vacuum variables
  to GcodeDriver, so if someone is depending on this functionality please speak up.

* BREAKING CHANGE: Placement Type "Ignore" has been moved to Placement Enabled. The Type field
  will be used only for specifying Placement or Fiducial, and a new Enabled field has been added.
  This makes it easier to disable placements for a job without losing whether that placement is a
  fiducial or a placement.
	
  Additionally, Type Place has been renamed to Placement.
  
  This change is backwards compatible but not forwards compatible. If you open and save a job using
  this version you will not be able to open it with an older version.
    
* BREAKING CHANGE: The Paste Dispense feature has been removed entirely. This feature is used by
  by very few people, does not work well, and is not well maintained. Removing the feature
  allows OpenPnP to focus on the pick and place user experience without having to maintain
  backwards compatibility with paste dispense.
    
  A version of OpenPnP with Paste Dispense will be saved and will be made available for users to
  download if they need that functionality for an existing setup. That version will receive no
  future updates.
  
* Added a new error handling system that allows users to specify on a placement by placement basis
  how errors should be handled. The options are Alert and Suppress. 
  
  Alert will raise the error immediately and pause the job. The user must fix the problem before
  continuing. 
  
  Suppress will mark the placement errored and continue on without alerting the user. When the
  job finishes the list of errored placements is printed. The user can then fix errors and re-run
  the job to fix unplaced placements.
  
  In the very near future there will be a GUI added that shows the errored placements at the end
  of the job so that you can see what needs to be fixed to finish the job.
  
* Removed Skip, Ignore and Continue, and Try Again from the Job error dialog. Now the error is
  shown and the job is paused. The user can make adjustments to the job and start it again to
  recovery from the error.

* Refactored vacuum sensing to Nozzle.isPartOn() and Nozzle.isPartOff(). Fixes #753, and #102.

* Removed the post pre-flight nozzle tip calibration from job processor. I can't see why this would
  be useful since the planner may immediately change out the nozzle tips. Calibration is now
  performed after tip changes for any tips that are not already calibrated. If this change is
  detrimental to your setup please speak up.    

* Added missing signaler signals for job running and stopped.  

* Increased the speed of the demo driver to make the demo a little more fun to watch.  
  
* Fix tooltips and button names on footprint pads.  

* Renamed Feeder.retryCount to Feeder.feedRetryCount.

* Added Feeder.pickRetryCount.

* Changed hardcoded pick retry in job processor to use Feeder.pickRetryCount.

* Coming Soon: There are a number of other features that are based on this work, which are coming
  very soon.
  
  * Production Tab: Job controls will be moved from the Job tab to a new Production tab. The
    Production tab will include status about the running job in total, and status for each
    placement being processed by the job.
    
    The Production tab will also include a list of placements that have errored during processing
    and will allow the user to quickly jump to the source of the error.
    
    The Placement Status and Placed? columns will also move to this tab. After this change the Job
    panel will be mostly used for Job editing before starting a job, and will be mostly read only
    during a job run.
    
# 2019-05-10

## Bottom Vision Pre-Rotate Updates and Bug Fixes 

* Bottom Vision Pre-Rotate now stores the rotation offset in the PartAlignmentOffset allowing 
  Pre-Rotate Bottom Vision with multiple nozzles sharing a C axis. 
* The angle calculation is unified and simplified with regard to math laws (distributive modulo equivalences).
* Pre-Rotate Bottom Vision is done in a multi-pass loop until a good fix is obtained.
* X, Y offsets are obtained together with the rotation offset, so with a good fix only one vision pass is needed 
  (formerly two needed). This way Pre-Rotate Bottom Vision becomes as fast as Post-Rotate in most cases. 
* The maximum allowed linear center and corner offset as well as the angular offset can be configured (GUI). 
  If the allowed offsets are not met, an additional compensation & vision pass is done. A maximum number of passes can be configured.
* The use of Pre-Rotate can now be overridden per Part (Default, Always on, Always off).   
* A bug in Test Align was fixed, where the part height was not added to Z i.e. the part held too close to the camera.

For more information see https://github.com/openpnp/openpnp/pull/815

# 2019-04-21

## Italian and French Translations

Thank you to Davide and Sebastien for adding Italian and French translations!


# 2019-02-26

## New Job Planner

A new Job Planner is being tested. The SimplePnpJobPlanner replaces the StandardPnpJobPlanner
as the new default. The new planner attempts to fill as many nozzles as possible per cycle,
and tries to limit the number of nozzle tip changes, but does not try as hard to "look ahead"
as the old planner.

The upside is that the new planner is much, much faster and works for jobs
of any size, while the old planner would fail on jobs larger than a few hundred placements
when multiple nozzles were in use.

The downside is that the new planner may perform more total cycles, and may perform more
nozzle tip changes, although in testing so far it seems to perform pretty similarly for
a number of common configurations.

The new planner is enabled by default. Please give it a try and report if you run into any
issues. If you need to switch back to the old planner, you can edit machine.xml and change

`<planner class="org.openpnp.machine.reference.ReferencePnpJobProcessor$SimplePnpJobPlanner"/>`

to

`<planner class="org.openpnp.machine.reference.ReferencePnpJobProcessor$StandardPnpJobPlanner"/>`

## New Job Planner Interface

As indicated above, the Job Planner interface is back and it is now possible to write custom
planners and plug them in using the method described above. This interface is not final, and
will likely undergo some small changes in the future, but the basic concept should remain
the same.

See either of the two planners described above for an example of how to write one.

# 2019-02-21

## High Profile Bugs Fixes and Updates

A few important bug fixes for long standing bugs are now in, along with some long standing
feature requests:

* "No defined transitions from Preflight to Initialize": There were a number of errors related
  to state management in the JobPanel FSM and these are now corrected. The primary cause of this 
  error was clicking Job buttons while an action was already taking place. State is now managed
  correctly, and more importantly, the buttons are disabled while operations that can't be 
  interrupted are taking place.
  
  This is a significant change that is hard to test under every condition, so please let
  us know if you run into issues with this.
  
  See https://github.com/openpnp/openpnp/issues/478 for more info.
  
* "Stopping a job should stop the job as soon as possible": This issue was related to how
  the job would continue for a time after pressing pause or stop. In general, once a pick
  and place cycle started it could not be interrupted. In addition, the startup process of
  a job would be impossible to interrupt. This is now fixed and granularity of steps is increased.
  Clicking pause or stop will now stop the job as soon as the current operation is complete.
  
  See https://github.com/openpnp/openpnp/issues/278 for more info.
  
* Multi-select tables and right click menus: All of the primary tables now support multi-select
  and right click menus. This makes it much easier to enable / disable a number of feeders at
  once, or set the "Check Fids" for multiple boards at once, for instance.
   

# 2019-02-18

## Major Change: Fiducial System (Affine Transforms)

The fiducial system has undergone extensive changes to support compensating for scale and shear
in boards. When three or more fiducials are available, three will be used during the fiducial
check and this data is used to calculate much better positions for placements. This should
result in an overall accuracy improvement when using three fiducials.

In addition, the two fiducial system is now using the same code, minus shear processing, so
it should show an improvement in accuracy as well when using two fiducials. The difference will
not be as significant as when using three.

Note that board locations are no longer updated when performing fiducial checks. The fiducial
data is used in real time to calculate placement positions, rather than relying on the board
location. This means that if you perform a fiducial check and then move to the board location,
the board may not look perfectly aligned, but when you move to a placement it will be correct.

The fiducial check will still position the camera over the located board origin at the end of
the process so that there can be visual verification of success.

In general, users should not notice any differences in using the new system aside from overall
better accuracy.

Related issues:
* https://github.com/openpnp/openpnp/issues/648
* https://github.com/openpnp/openpnp/issues/791

This above changes are complete. There are a few remaining related tasks which are:
* Implement transform for the two point board locator.
* Add an indication to the user when a board has been located.
* Verify compatibility with panels.
* Implement inverted transform in calculateBoardPlacementLocationInverse so that manual training
  benefits from the transform.


# 2019-02-14

## New GcodeDriver Variables

Some new GcodeDriver variables have been added for the MOVE_TO command. The new variables are used
for heads where the controller needs to know the direction of motion to choose the right output.

More information at: https://github.com/openpnp/openpnp/wiki/GcodeDriver:-Command-Reference#move_to_command

# 2019-02-12

## Breaking Change: Park System

**Please reset your park locations!**

The Park system has had a number of breaking changes made. They are:

* Park XY now always parks the head at the same location, regardless of what tool is selected in
  the jog control dropdown. This ensures that if you choose a park location with one tool selected
  and then attempt to park with a different tool selected you don't crash the head. This is
  primarily a safety improvement.
* Z and Rotation have been removed from the park head configuration since these are each specific
  to the tool being parked.
* Park Z now parks the selected tool at Safe Z instead of the Z entered in the head configuration.
* Park Rotation now parks the selected tool's rotation at 0 instead of the Rotation entered in the
  head configuration.

The overall goal and result of these changes is to ensure that park always parks at the exact same
location no matter what tool is selected.

See https://github.com/openpnp/openpnp/issues/279 for more information. 

## New Feature: XY Soft Limits

You can now set soft limits for X and Y moves in head configuration. When limits are set and
enabled any attempted moves outside of the limits will fail and an error will be shown.

# 2019-02-09

## Breaking Changes Coming Soon

A number of breaking changes are coming soon in the develop / Latest branch. These changes will
likely break configurations and potentially require re-setup and re-calibration of machines.

If you are seeing this message it means you are currently running the develop / Latest branch.
In the coming weeks, if you automatically update on this branch you are likely to receive
these breaking changes.

If you are running a production machine and do not wish to follow along with these latest
developments, please download and install the master / Stable branch from:

http://openpnp.org/downloads/

Stable has been updated as of today and will not be updated again until these breaking changes
have been fully tested and released.

# 2019-01-18

* Runout Compensation Feature Enabled

    There has been worked on issue #235 in pull request #804 to fix the nozzle runout compensation.
    It was a new runout compensation algorithm implemented. That algorithm is the new default but it
    coexists with the improved algorithm that was in the OpenPnP code before already.
    
    The feature was tested on two machines, but things are different on others. If you encounter any
    problems file an issue. Information on how to use this feature you will find in the wiki at
    https://github.com/openpnp/openpnp/wiki/Runout-Compensation-Setup
    
# 2018-12-08

* Serial Library Change

    The library that OpenPnP uses to communicate with serial ports, jSSC, has become out of date and is
    unmaintained, so we're trying a new library. The new library is jSerialComm:
    https://github.com/Fazecast/jSerialComm.
	
	This change should not affect existing users, so if you notice new problems with serial port access,
	please file an issue or post to the mailing list.    
    

# 2018-11-17

* OpenCV 3.4.2 Upgraded

	The bug in the OpenCV library has been fixed so once again OpenPnP is upgraded to OpenCV 3.4.2.
	
* OpenPnP Compatible with Java 9, 10, 11.

	With the upgrade of OpenCV OpenPnP is now compatible with all versions of Java after 8. This includes
	Java 8, 9, 10, and 11. 

# 2018-10-31

* OpenCV 3.4.2 Upgrade Reverted

	The OpenCV upgrade has temporarily been reverted due to an issue found in the OpenCV library
	during testing:
	
	https://github.com/openpnp/opencv/issues/38
	
	Once this issue is resolved, this patch will be re-added.
	
# 2018-09-26

* Connection Keep Alive

	Thank you to @markmaker for PR https://github.com/openpnp/openpnp/pull/767 which adds a keep
	alive option to the drive communications configuration. This option, which is on by default,
	can be turned off to cause OpenPnP to close the serial port or TCP port when clicking the
	disable button. This makes it possible to connect to the serial port from another program
	without having to exit OpenPnP.
	
	Note, again, that this option is on by default which is the pre-existing behavior. You can turn
	it off if you want the new behavior.
	 
# 2018-09-10

* Nozzle Offset Setup Wizard

	There's a new nozzle offset setup wizard in the Nozzle setup area that now makes it very easy to
	setup nozzle offsets. This is one of the more confusing aspects of setting up OpenPnP and the new
	wizard makes it very easy. Many thanks to @pfried for this new feature!
	
	See https://github.com/openpnp/openpnp/pull/765 for more information.

# 2018-08-18

* Placements Comments

	The Placements table (Pick and Place) now contains a user editable Comments column that is
	saved in the board file for each Placement.
	
# 2018-08-04

* OpenCV Upgraded to 3.4.2

	OpenCV has been upgraded to 3.4.2, which is the latest release.
	
# 2018-07-15

* TCP/IP Support in GcodeDriver

	Thanks to a great effort from @PeeJay, GcodeDriver (and the OpenBuilds driver) now support
	communication over TCP/IP in addition to serial. This makes it possible to use Smoothieboard
	over Ethernet now, for example. To use TCP/IP, go to your Driver settings and check the
	Communication tab for new options.
	
	This change requires a migration of communication settings. This should happen automatically.
	If it doesn't, or if you get an error on startup, please let us know and post your machine.xml
	to the Discussion Group at https://groups.google.com/forum/#!forum/openpnp.

# 2018-07-13

* Dwell Times per Nozzle Tip

	Pick dwell time and place dwell time has been added to nozzle tip. 
	This means the total dwell times are now the sum of the nozzle dwell times 
	plus nozzle tip dwell times. The idea behind this is that larger nozzle tips
	are used to lift bigger/heavier chips and typically require a bit longer dwell
	times in general.
 
# 2018-07-08

@aneox submitted a bunch of great new features. Some are still being worked on, but the following
ones have been merged in:

- Filter PlacementsTableModel, show only active board side. Note that this change makes it so
  that if you want to edit both sides of a board you have to add it to the job twice and set
  the side.
- Added option to AutoHome after machine enabled. To activate, need to set checkbox in machine settings.
- Windows saves sizes and position in Multiple Windows Mode.
- Save configuration menu button. (Moved from Machine menu to File menu)
- Camera window can be split in vertical or horizontal style.
- Job autosave after each placement. Please post to the list if this causes a performane issue
  on your machine.
  
 The following items have been merged but some additional work may still need to be done on them:
  
- Added peel off actuator option for Drag Feeder.
- Drag Feeder improve accurance of feed, now drag distance can be adaptive with vision enabled.
- Drag Feeder can work with 0402.

Thanks @aneox for all the great work!

# 2018-07-04

* Placement Status Indicator and Progressbar

	A new panel in the bottom status bar has been added that lists the current jobs
	total number of placements, completed number of placements and the same values 
	for the selected board only. A progress bar shows the percentage of completed 
	placements for the entire job. These indicators update in real-time whenever 
	any placement/board is edited or while the job runs.

# 2018-07-02

* Machine -> Save Config

	A new menu option called Machine -> Save Config does a force save of the machine.xml,
	parts.xml, and packages.xml.

# 2018-01-28

* OpenPnpCapture New Properties

	The OpenPnpCaptureCamera now supports new properties for backlight compensation, hue,
	powerline frequency and sharpness. 

# 2017-12-23

With thanks to @mgrl:

* Bugfix: Discard correct nozzle on skip part

	If an error raised while job run (vacuum sense/bottom aligning failed), the first nozzle was cleared always
	regardless which nozzle failed in a multi nozzle setup.	This is fixed now.w
	
	See:
		* https://groups.google.com/d/msg/openpnp/x249mhevB3U/DSJg2fyVBAAJ
		* https://github.com/openpnp/openpnp/pull/693
		
* Bugfix: Setting placed flag correctly (fixes #663)

	There is now a fix having the placed flag set correctly if fiducials checking is enabled.
	
	See:
		* https://groups.google.com/forum/#!topic/openpnp/4MKg7JaUTAk
		* https://github.com/openpnp/openpnp/issues/663
		
* Enhancement: Added option to ignore error and continue assembly
	
	To handle errors in a running job, next to Try Again, Skip and Abort there is a new option "ignore and continue".
	It continues a running job as if no error has been occurred (e.g. vacuum check/bottom vision failed).

	See:
		* https://groups.google.com/forum/#!topic/openpnp/x249mhevB3U 
		* https://github.com/openpnp/openpnp/pull/688

* Bugfix: A feeder feeder with no part assigned doesn't throw an NullPointerException if try to edit pipeline due to missing part name 

	See:
		* https://github.com/openpnp/openpnp/pull/689
	
# 2017-11-20

* Manual NozzleTip Changing Fixes

	Thanks to @netzmark there is now a fix for manual nozzle tip changing. Now, if you do not
	have auto changing turned on, when the job attempts to change nozzles, the job will be
	paused and you will be shown a message indicating the change.
	
	See:
		* https://groups.google.com/d/msgid/openpnp/00ead7a9-e7d5-49e8-856c-2a403208058d%40googlegroups.com?utm_medium=email&utm_source=footer
		* https://github.com/openpnp/openpnp/issues/118
		* https://github.com/openpnp/openpnp/issues/526
	
# 2017-10-26

* Fiducial Locator Retry and Averaging

	With thanks to @mgrl, retry count on the fiducial locator, which was previously fixed at
	3 is now configurable in Machine Setup -> Vision -> Fiducial Locator.
	
	In addition, a new option is added which allows averaging the results from the retries. This
	helps alleviate some jitter that happens as the results shift with the movement of the
	camera. 

* ReferenceLoosePartFeeder Improvements

	There is a new default pipeline that performs well for non-polarized, rectangular
	components such as resistors and capacitors. 
	
	It attempts to include the electrodes as well as the bodies to better recognize rectangular
	parts and it performs landscape orientation on the results so that there is a deterministic
	orientation for rectangular parts.
	
	The camera feedback is now only shown at the end of the process, and for a longer
	time. This better represents what OpenPnP is "seeing" before it picks the part.
	
	The feeder's rotation defined on it's location is now added to the final rotation so that
	you can set the orientation you want parts picked in.
	
* New CvPipeline Stage: OrientRotatedRects

	The new stage ensures the orientation of RotatedRects is set to either landscape or
	portrait. This is used in the above pipeline. 
	
	In addition, you can set a flag to negate the angle of the RotatedRects. This is 
	primarily used when converting from the output of MinAreaRect to what OpenPnP expects for 
	Locations.


# 2017-10-25

* CvPipeline Editor Model Info

	The pipeline editor will now show some information about any identified models it finds
	as you move the mouse around the result window. 
	
	For instance, if the result you are viewing
	includes a List<RotatedRect> and you mouse over the center of one of them in the image view,
	you will see the description of that RotatedRect in the status field. 
	
	This is very helpful for learning more about what is happening in your pipelines and makes it
	easy to debug model data.
	
	This feature currently works for RotatedRect, Circle, and KeyPoint models, and Lists of the
	same.
	
	This video shows the feature in action: https://www.youtube.com/watch?v=sHuUPtJNIXw
	
* New CvPipeline Stage: Add

	A new stage has been added for use in pipelines. The stage is called Add and it simply
	outputs the sum of two previous images. This is used in a new Loose Part Feeder pipeline
	that will be released soon.
	
* CvPipeline Standalone Editor Pipeline Restore

	The  CvPipeline Standalone Editor will now save and restore the last pipeline you were
	working on, similar to how the last directory you were working on is saved.

# 2017-10-24

* CvPipeline Memory Usage Improvements

	CvPipeline now implements AutoClosable and all of the code that uses it has been updated to
	release after use. This should greatly improve memory usage on large jobs with many parts.
	
* ReferenceBottomVision Improved Error Messages

	ReferenceBottomVision will now throw specific error messages for common pipeline setup errors
	such as an improperly named result stage or an invalid result type.
	 
# 2017-10-21

* GcodeDriver Axis Pre Move Command Coordinate Variable

	Pre Move Command in GcodeDriver Axes now has a Coordinate variable which can be used to reset
	an axis' position before moving it. This can be used in controller firmwares that do not
	support individual variables for multiple axes. In particular, this makes it possible to
	use Marlin with multiple rotation axes by using a Pre Move Command like
	`T0G92E{Coordinate:%.4f}`
	
# 2017-10-18

* Vision Usability Improvements

	As a result of the discussion in https://groups.google.com/d/msgid/openpnp/7029bade-fa16-46e5-8c2d-d9e22105c5fe%40googlegroups.com?utm_medium=email&utm_source=footer several changes have been made to
	the vision pipeline system.
	
	* ReferenceBottomVision now looks for it's results in a stage named "results", like the
	other primary vision operations. It also falls back to "result" for backwards compatibility.
	* ReferenceBottomVision now has improved error messages when a result is not found, or when
	the result in not in the correct format. This should help users as they experiment with
	new pipelines.
	* Vision operations all now reference a common name to avoid mistakes like this in the future.

# 2017-10-05

* OpenPnpCaptureCamera Updates

	* Implemented the rest of the camera properties.
	* Camera properties now refresh when changing device or format.
	* Auto disabled state now reflects if auto is supported.
	* Added display of default value.

# 2017-09-30

* Major Update: New Camera Capture System!

	OpenPnP now has it's very own, custom written camera capture system written specifically to
	solve all of the problems that have plagued camera capture since the beginning of this project!
	
	openpnp-capture is a brand new capture library written by Niels Moseley (@trcwm) specifically
	for OpenPnP. Using this library we are now finally able to do things like run multiple USB
	cameras on a single port/hub, manage camera properties such as exposure, focus and white
	balance and select camera data formats to make intelligent choices based on quality, size, 
	frame rate, etc.
	
	Two of the biggest difficulties with capture in OpenPnP from the start have been the
	inability to run multiple cameras over a single USB port/hub and the inability to control
	manual exposure. The first is important because many people use OpenPnP with laptops
	which may have a limited number of ports. The second is important because most commercial
	USB cameras default to auto exposure and this causes problems with vision as the camera
	adjusts the exposure to compensate for differences in the image.
	
	Using the new library, you can now set up your lighting and choose the exact exposure that
	works best for your machine, and you will know that it won't change just because the
	camera is looking at something else.
	
	To use the new feature, add a new camera using the OpenPnpCaptureCamera and see the General
	Settings tab to select a device, format and property settings.
	
	I want to give a HUGE shout out and thank you to Niels for all his incredibly hard work
	on the new capture library over the past couple months. He wrote a robust and expansive library
	for video capture for all three major operating systems in a very short period of time,
	knocking out feature after feature faster than I could integrate them into OpenPnP. This is
	an invaluable contribution to the project and will really push OpenPnP forward in it's
	computer vision abilities. Thank you Niels!
	
	For more information about the capture library itself, see:
	https://github.com/openpnp/openpnp-capture
	
	For information about the Java and Maven bindings for the capture library, see:
	https://github.com/openpnp/openpnp-capture-java
	
	Finally, be aware that there are some known issues:
	* When you switch the selected device, the wizard doesn't reload the properties. To work
	around, simply click to another wizard and then click back. To be fixed soon. 
	* Brightness, contrast, saturation, gamma properties not yet implemented in OpenPnP. These
	properties were recently added to the capture library but have not yet been implemented
	in OpenPnp. 
	
	If you run into any other issues, please file a bug report or post to the mailing list. Your
	feedback will help us make this new feature even better! 

# 2017-09-21

* Ctrl-Shift-L Hotkey Added for Park Z

# 2017-09-16

* Job Save Always Enabled

	The File -> Save Job menu option is now always enabled so that you can save the job
	and any associated boards at any time. Previously this was only enabled when the
	board was marked dirty, and it did not reflect the status of the associated boards which
	made it hard to save boards on demand.
	
* Camera FPS in Image Info

	The Image Info pane in the camera view now shows current FPS being received from the
	camera. This was put in for testing some new features but was useful enough that I
	decided to leave it in so users can check their camera feeds.


# 2017-08-31

* Job Placed Status

	Placements now have a Placed column that indicates if the placement has been placed.
	This value is saved with the job, so it is now possible to do partial assembly, exit
	OpenPnP, and then recover the job from where you left off.
	
	You can right click the placements table to perform a bulk set or reset of the Placed flag
	and there is a new Job menu item that will reset the Placed status for the entire job at
	once. This can be used to quickly prep the job to be run again after it's finished. 

	Associated issues:
	https://github.com/openpnp/openpnp/issues/205
	https://github.com/openpnp/openpnp/issues/258
	
	Implemented in PR https://github.com/openpnp/openpnp/pull/632
	
	Thanks to @sp-apertus for this huge improvement in usability!
	  
# 2017-08-29

* ReferenceStripFeeder Improvements

	* Added auto-thresholding to the default CvPipeline for ReferenceStripFeeder to better
	detect tape holes and eliminate false-positives in noisy camera images. Users should
	reset their feeder vision pipelines to the default to get this change, then re-apply
	any pipeline changes if still necessary.
	* Auto Setup for ReferenceStripFeeder is now a lot smarter, more accurate, and is able
	to catch common setup issues.
	* Fixed issue where strips with 2mm part pitch could result in the reference holes being
	detected flipped depending on where on the two parts the user clicked.
	* Fixed issue where part pitch was calculated in the units of the camera, not
	necessarily millimeters.
	* User is notified if they selected parts in the wrong order for the orientation of the
	strip.
	* Tightened the max distance from a component center to the feed hole centers to
	accurately reflect the spacing as defined in the EIA-481 standard and thus reduce
	false-positives for adjacent strips.
	* Multiple, full lines of strip holes are detected and grouped appropriately, and only
	the correct line of holes are used for the selected parts/strip (some spacing is still
	required between adjacent strips, but it is much reduced and more reliable).
	
	Implemented in PR https://github.com/openpnp/openpnp/pull/628
	
	Thanks to @richard-sim for these improvements!

* Head Movement Speed Limiting

	Head movements are now limited to the speed of the slowest part on the head at any
	time. This means that if you have more than one nozzle, and you have picked more than
	one part, if one part has a slower speed setting than the other, the slower one will
	dictate the speed of the head. Movements initiated by Cameras and Actuators on the same
	head will be limited in the same fashion.
	
	Implemented in PR https://github.com/openpnp/openpnp/pull/630
	
	Original issue https://github.com/openpnp/openpnp/issues/576
	
	Thank you to @johngrabner for this nice improvement!
	 
# 2017-08-28

* ReferenceStripFeeder Improvements

	* Added auto-thresholding to the default CvPipeline for ReferenceStripFeeder to better
	detect tape holes and eliminate false-positives in noisy camera images. Users should
	reset their feeder vision pipelines to the default to get this change, then re-apply
	any pipeline changes if still necessary.
	* Auto Setup for ReferenceStripFeeder is now a lot smarter, more accurate, and is able
	to catch common setup issues.
	* Fixed issue where strips with 2mm part pitch could result in the reference holes being
	detected flipped depending on where on the two parts the user clicked.
	* Fixed issue where part pitch was calculated in the units of the camera, not
	necessarily millimeters.
	* User is notified if they selected parts in the wrong order for the orientation of the
	strip.
	* Tightened the max distance from a component center to the feed hole centers to
	accurately reflect the spacing as defined in the EIA-481 standard and thus reduce
	false-positives for adjacent strips.
	* Multiple, full lines of strip holes are detected and grouped appropriately, and only
	the correct line of holes are used for the selected parts/strip (some spacing is still
	required between adjacent strips, but it is much reduced and more reliable).
	
	Implemented in PR https://github.com/openpnp/openpnp/pull/628
	
	Thanks to @richard-sim for these improvements!

# 2017-08-19

* New Scripting Event: Job.Placement.Complete

	New Scripting Event fired when a placement is complete, i.e. a part has been placed.
	
	See https://github.com/openpnp/openpnp/wiki/Scripting#jobplacementcomplete for usage.
	
# 2017-08-16

* ReferenceStripFeeder Converted to CvPipeline

	The vision operations for ReferenceStripFeeder have been converted from hard coded
	algorithms to use the CvPipeline system, as bottom vision and fiducial finding do. This
	makes it possible for you to easily customize the pipeline used for feeder vision to
	better match the conditions on your system.
	
	Implemented in PR https://github.com/openpnp/openpnp/pull/610
	
	Many thanks to @richard-sim for taking on this complex and important conversion! 

# 2017-08-15

* Board Jog Crash Protection

	A new tab called Safety has been added, with a checkbox that allows you to enable/disable
	board crash protection. This feature will throw an error if you try to jog a nozzle into
	a board.
	
	Implemented in PR https://github.com/openpnp/openpnp/pull/598
	
	Thank you to @machinekoder for this helpful improvement!

* Kicad Importer Improved Part Creation

	A new checkbox in the Kicad importer allows you to specify that only the value should
	be used when creating part names.
	
	Implemented in PR https://github.com/openpnp/openpnp/pull/620
	
	Thank you to @KarlZeilhofer for this new feature!

# 2017-07-30

* Additional Keyboard Shortcut Support

	Several new keyboard shortcuts have been added in an effort to support external control of
	OpenPnP. The new hot keys allow you to start, step and stop jobs, adjust jog increments and
	several other useful functions. For full details see the user manual:
	
	https://github.com/openpnp/openpnp/wiki/User-Manual#keyboard-shortcuts
	
	Implemented in PR https://github.com/openpnp/openpnp/pull/609
	
	Thank you to @yaddatrance for this helpful improvement!

* CvPipeline Editor Result Pinning

	Pipeline editor now supports pinning a stage's output so you can see how changes in
	other stages affect the pinned one. Select any stage and then click the Pin icon in the
	results panel to pin that stage. Selecting any other stage will let you edit that stage
	while seeing the results of the one you pinned. Click the pin icon again to turn it off.
	
	Implemented in PR https://github.com/openpnp/openpnp/pull/612
	
	Thank you to @richard-sim for this awesome improvement!
	
* CvPipeline Editor Null Value Fix

	Fixes issue #597 which caused the pipeline editor to sometimes set values to null when
	changing stages. This bug often caused corrupt stage data and made it impossible to save
	stages.
	
	Fixed in PR https://github.com/openpnp/openpnp/pull/611
	
	Many thanks to @richard-sim for tracking down and fixing this bug!

# 2017-07-15

* Code Cleanup: Potential Breaking Change

	As part of a scheduled code cleanup several old configuration settings have been removed. If
	you have upgraded within the past few months you should not see any change, but if your
	configuration is very old it may fail to load with this version. If you get an error
	starting OpenPnP after upgrading to this version, please look for and remove the following
	lines from your machine.xml:
	* `glue-dispense-job-processor`
	* `vacuum-request-command` See https://www.youtube.com/watch?v=FsZ5dy7n1Ag
	* `vacuum-report-regex` See https://www.youtube.com/watch?v=FsZ5dy7n1Ag
	* In board files: `glue` attribute.
	
	If you have any trouble with this please post to the mailing list for help.
	
# 2017-07-02

* Improved Nozzle Changer Speed Support

	With thanks to @lilltroll77 we now have improved nozzle changer speed control. The speed
	controls added recently had a limitation where different speeds would be used for different
	parts of the movement. You can now define three speeds that are used between the four
	movements and they are applied during those transitions whether it is for load or unload.
	
	Note that since the configuration has changed slightly for this feature, you should
	check your speed settings before running a nozzle change with this new version. Settings
	should be migrated over automatically, but it is prudent to check them before using.
	
	More information about this change is available at:
	https://github.com/openpnp/openpnp/issues/584

* Fiducial Vision Converted to CvPipeline

	The fiducial vision system has been converted to use the CvPipeline system as per
	https://github.com/openpnp/openpnp/issues/329.
	
	This allows users to easily edit the vision pipeline for fiducials, making it easy to
	customize for different board and lighting scenarios. Pipeline editing works the same
	as in bottom vision; you can edit the pipeline on a part by part basis or at a global
	default.
	
	The default pipeline included with OpenPnP is an exact duplicate of the code that used to
	be used internally - it has just been converted to pipeline form to make it editable.
	
	If you notice a degradation in fiducial performance, please post a message to the
	mailing list at http://groups.google.com/group/openpnp

# 2017-06-30

* Power On, No Home Behavior

	Now when you hit the power on button the home button becomes highlighted to indicate you should
	home the machine. Previously the power button would change color which was confusing. 

* SimulatedUpCamera Rewrite

	The SimulatedUpCamera has been rewritten to work much better. It is now included in the default
	configuration so that you can test out bottom vision before you have a machine. It's also
	been made testable, so there is now test coverage for basic bottom vision operations.

# 2017-06-28

* CvPipeline Properties (Breaking Change)

	In an effort to make it easier for developers to integrate custom functionality in CvPipelines,
	the pipeline now has a map of properties that can be set be callers. This allows callers of
	a pipeline to feed values in for the pipeline to use. This can be things like cameras, feeders,
	parts, nozzles, etc. 
	
	This functionality replaces the previously added setFeeder and setNozzle calls. These calls
	were too specific to certain pipelines and did not represent a good development direction
	for the pipeline as it would eventually become cluttered with variables that did not
	make sense for the pipeline as a whole.

	Breaking Change: All existing stages have been migrated to the property system. If you have
	custom stages that used getNozzle or getFeeder you will need to make minor updates to switch
	these to use properties instead.
	
	* getNozzle() becomes (Nozzle) getProperty("nozzle")
	* getFeeder() becomes (Feeder) getProperty("feeder")
	
	Finally, this change is the first step into supporting variables in CvPipeline. Eventually
	you will be able to reference properties and other objects when setting parameters in stages.
	 
* AdvancedLoosePartFeeder

	ReferenceLoosePartFeeder has received a big upgrade thanks to @dzach. The
	new AdvancedLoosePartFeeder is able to be trained to recognize the orientation of loose parts,
	allowing perfect placement of loose bins of both polarized and unpolartized parts. This
	provides a complete feeding solution with no feeders at all!
	
	A lot of work and discussion has gone into this feature. For more details see:
	https://github.com/openpnp/openpnp/issues/573#issuecomment-311633280
	https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/openpnp/zqeeh6mGqtk/Ix9MgDbvCAAJ
	
	It is expected that the default pipelines will need to be tuned and updated as we
	get more experience with this new system. Please post your feedback about this feeder
	to the mailing list.
	
	Thank you @dzach!
	
# 2017-06-17

* Nozzle Tip Changer Speed Settings

	Nozzle Tip Changer now has independent speed settings for each movement. The speeds are a 
	multiplier, similar to how it's used in other parts of the system. The value
	is multiplied by the system speed slider to determine the final speed. A 1.0 is "full speed".

# 2017-05-18

* New Scripting Events

	Two new Scripting events have been added: Job.Starting and Job.Finished. These are called
	as the job is starting and after it completes. They are intended to aid in using conveyer
	systems to automatically load and unload PCBs.
	
	See https://github.com/openpnp/openpnp/wiki/Scripting#jobstarting for more info.

# 2017-05-15
	
* New tray feeder added: RotaryTrayFeeder

	This tray feeder takes 3 points (first component, first row last component, last row last component) 
	to measure the component grid and is rotation agnostic. Feedback and experience reports are welcome.

# 2017-05-07

* Configuration Wizard Performance Improvement

	Due to a bug in a third party library that is used extensively in the configuration wizards
	in OpenPnP, performance on opening the wizards was often very poor for many users. This was
	most obvious when clicking through your various feeders, where some users were experiencing
	up to a 10 second delay in opening the wizard.
	
	Unfortunately, the library has been abandoned so even though there is a fix available, it
	will likely never be released. Instead, we are now "monkey patching" the fix at runtime
	and this solves the problem.
	
	Thanks to @SadMan on IRC for putting me on the path to the fix.
	
# 2017-05-06

* New Bottom Vision Scripting Events

	Two new scripting events have been added to assist with bottom vision lighting. They are
	Vision.PartAlignment.Before and Vision.PartAlignment.After.
	
	See https://github.com/openpnp/openpnp/wiki/Scripting#visionpartalignmentbefore for more
	information.

# 2017-04-16

* Script Directory Ignore

	You can now add an empty .ignore file to any directory under the scripts directory to
	have that directory be ignored when populating the Scripts menu. This is in support of
	a feature by @cri-s to improve usability on production machines.
	
	More information at https://github.com/openpnp/openpnp/pull/521.
	
* Home Status
	
	The "Power On" button now turns yellow when you first enable the machine, and does not
	turn green until the machine is homed. This helps you notice that you have not yet homed
	the machine. Thanks to @ldpgh for this helpful feature!
	
	More information at https://github.com/openpnp/openpnp/issues/379.
	
* Python Script Examples Added

	@ldpgh has added some helpful Python examples to the suite of built in Scripting
	examples.

	More information at https://github.com/openpnp/openpnp/pull/520.
	
# 2017-04-14

* Navigation View Removed

	The Navigation View has been removed as part of a cleanup effort. This feature was unfinished
	and is unlikely to ever be finished in this iteration of the UI. Removing it improves startup
	time, removes a dependency on JavaFX and solves some bugs.
	
	If you were using this feature and will miss it, please make it known on the mailing list
	at http://groups.google.com/group/openpnp.
	
# 2017-04-13

* BREAKING CHANGE: Outdated Drivers Removed

	Several outdated drivers have been removed. These are: GrblDriver, MarlinDriver, SprinterDriver, and
	TinygDriver. All of these drivers have been replaced with the much better supported
	GcodeDriver. If you are currently using one of these drivers this version WILL BREAK your
	configuration. If you need help migrating, please post a question to the mailing list at:
	
	http://groups.google.com/group/openpnp
	
	More information about this change and the reasoning for it is available at:
	
	https://github.com/openpnp/openpnp/issues/415
	

# 2017-04-09

* Filter Rotated Rects CvStage

	A new pipeline stage called FilterRects has been added by @dzach. It allows you to filter
	rotated rects based on given width, length and aspect ratio limits. This can be very helpful
	for making sure a recognized part is within acceptable size limits.

# 2017-04-06

* Tool Selection for Cameras

	Thanks to @BendRocks an old feature has been brought back to life. You can now select
	head mounted cameras from the Machine Controls tool dropdown box. This causes the DROs
	to show the coordinates of the camera and allows you to jog from the camera's perspective
	instead of just the nozzle's. This also makes it possible (although not yet implemented)
	to do the same kind of thing for paste dispensers when that feature is revived.
	
	Work for this feature was performed in: https://github.com/openpnp/openpnp/pull/507
	
# 2017-04-01

* Auto Panelization

	Thanks to @BendRocks we now have a robust panelization solution in OpenPnP! Panels allow you
	to quickly set up multiple copies of a board in an array and allow you to have panel
	fiducials in addition to board fiducials. There is also a quick X out feature that makes it
	easy to mark boards in the array that are damaged and should not be placed.
	
	This feature is a work in progress. There are some known issues and some limitations
	but it has matured enough that it's ready for people to start trying it out.
	
	Full documentation for this feature is coming soon and will be available at:
	https://github.com/openpnp/openpnp/wiki/Panelization
	
	For more information about this feature, please see the following links:
	https://github.com/openpnp/openpnp/issues/128
	https://github.com/openpnp/openpnp/pull/456
	https://groups.google.com/forum/#!msg/openpnp/_ni0LK8LR8g/5u-0-P-1EwAJ;context-place=forum/openpnp

# 2017-03-31

* Job Placement Status

	With many thanks to @iAmRoland we now have a great status display of placements as a job
	is run. Their description from the pull request describes the feature nicely:
	
	> Once the start button is pressed, it will mark all pending placements with a yellow color. 
	> When it's processing a placement it will display a blue color on the ID cell. Once it's done
	> with all placements with the same ID, it then marks that cell with a green color. If no
	> placement is going to be done then the cell is left white.
	
	@iAmRoland even included a nice GIF that shows how it looks:
	https://camo.githubusercontent.com/954ded479f650507bece8c199c7b73233708097e/687474703a2f2f692e696d6775722e636f6d2f6d6c4130716d6b2e6a7067
	
	This work was performed in PR https://github.com/openpnp/openpnp/pull/493 and partially
	addresses the feature described in issue https://github.com/openpnp/openpnp/issues/205 and
	https://github.com/openpnp/openpnp/issues/280.

# 2017-03-26

* Auto Update Fixed, Version Number Improvements

	An error that was causing the auto updater to not work has been fixed. In the process,
	the OpenPnP version numbering scheme has been changed and improved. Version numbers were
	previously just an inscrutable Git hash. They are now in the format of
	2017-03-26_18-56-32.0be8a03, with the part before the period representing the date of the
	build and the part after the period representing the Git hash. This makes it easy to
	identify when the code was built and how old it is, and the Git hash can be used to
	identify a specific commit.
	
* Glue Feature Removed

	The Glue Dispense feature has been deprecated and removed. This feature was not being used
	and it was causing maintainability problems. If there is interest in the feature in the
	future it will be rewritten. More information about this decision is available at
	https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/openpnp/1-CSpnoPQGY/k6qUT9VZAQAJ

# 2017-03-21

* Submit Diagnostics

	You can now submit a very detailed diagnostics package about OpenPnP right from OpenPnP itself!
	In the Help menu just click Submit Diagnostics and follow the instructions. The result will
	be a link to a [Gist](https://gist.github.com/) on Github that contains information and
	images from OpenPnP. You can share the link on the mailing list or elsewhere to assist
	people in helping with any issue you might be having.

# 2017-03-16

* Named CSV Importer Improvements

	Thanks to @Misfittech the Named CSV Importer can now handle Altium R14 files and now
	has the option to set part heights when available. It also supports the ability to
	import data that contains values in mils.
	 
* Logging Improvements

	With many thanks to @pfried the Log tab and the logging system have seen several improvements:
	* Log panel is now searchable.
	* Select and copy lines from the log.
	* Enable/disable system output.
	* Option to auto-scroll, or not.
	
	More information about this feature can be found at:
	https://github.com/openpnp/openpnp/issues/288

# 2017-03-05

* Force Upper Case in Gcode Driver Console

	There is now an option, on by default, that forces commands sent from the Gcode console
	to upper case. Previously upper case was forced without option, but now you can turn it off
	if you like.

# 2017-03-04

* Position Camera and Continue

	Thanks to @BendRocks for two new buttons in the Job and Placements panel. The buttons
	which look like the Position Camera button with a right arrow added allows you to
	position the camera and then select either the next board or placement. This allows you
	to very easily and quickly move through a job and see that all of your placements
	are configured correctly.
	
* Console Output in Log Panel

	Thanks to @pfried, console output (System.out, System.err) is now included in the Log
	panel at the Info and Error levels respectively. One major benefit of this is that
	scripting output will now be visible in the Log panel. 

# 2017-02-27

* ReferenceDragFeeder Configuration Actuator Positioning

	Fixes a bug in the ReferenceDragFeeder configuration panel that kept the actuator positioning
	buttons from showing up. Now when you set an actuator name the position nozzle buttons on
	these fields will turn into position actuator buttons as they did previously.

* GcodeDriver Sub-Driver Delete UI

	You can now delete sub-drivers from the UI by selecting one and clicking the red X button
	in the toolbar above.
	
# 2017-02-24

* HTTP Actuator 

	A new boolean actuator that calls a predefined URL for ON and OFF events. It was developed 
	for controlling pneumatic feeders that are controlled via a Raspberry Pi with IO shield but
	maybe there are totally different applications as well.
	
* GCode Backlash Compensation

	The GCode Driver now features a few additional parameters to address backlash. 
	This allows approaching target locations always from a specific direction on X/Y axes.
	Optionally the final approach can be executed with reduced speed. 
	Details: https://github.com/openpnp/openpnp/wiki/GcodeDriver#user-content-backlash-compensation
	
* GCode Console

	The GCode Driver now features a new tab to manually send GCode commands in a console.
	
* GCode Non-Squareness Compensation

	The GCode Driver now also works with machines that are not perfectly square. Details about
	how to measure and compensate this Non-Squareness Compensation can be found here:
	https://github.com/openpnp/openpnp/wiki/GcodeDriver#user-content-non-squareness-compensation	
	
# 2017-02-12

* Generalized Vacuum Sensing (BREAKING CHANGE)

	Vacuum sensing was previously a GcodeDriver only feature. With the recent Actuator
	Improvements it became possible to extend this feature to all drivers. The vacuum
	sense feature now uses an Actuator to read values from the pressure sensor, instead
	of a specialized GcodeDriver command.
	
	Configuration is still similar. Instead of defining a VACUUM_REQUEST_COMMAND and
	VACUUM_REPORT_REGEX you just create an Actuator that uses the same values
	and set the Actuator name on your nozzle.
	
	Due to this configuration change, this is a breaking change. Your vacuum sense
	will not work until you make the manual changes. You can watch a short video tutorial
	showing how to make the required changes at: https://www.youtube.com/watch?v=FsZ5dy7n1Ag

# 2017-02-05

* Actuator Improvements

	* Actuators can now read String values in a generic fashion. This makes it possible to
	integrate a variety of sensors into your system and use the output in any way you like,
	particularly with scripting. The GcodeDriver has been updated to work with this new
	functionality. For more information see:
	
	https://github.com/openpnp/openpnp/wiki/GcodeDriver#actuator_read_regex
	
	https://github.com/openpnp/openpnp/wiki/GcodeDriver:-Command-Reference#actuator_read_command

	* The Actuators panel in Jog Controls now offers more options for controlling and testing
	actuators. You can send true/false boolean values, send double values and read a response
	from each actuator.

# 2017-01-27

* Icon Improvements

	With many thanks to @dzach we now have some much improved icons in many parts of OpenPnP.
	@dzach spent several days working on and refining icons to show their intent better, and
	to improve usability to persons with color blindness. As a result, several of the icons
	that used to require you to think for a moment to remember what they were are redesigned
	to be very clear and easy to understand.
	
	References:
	https://github.com/openpnp/openpnp/pull/429
	https://github.com/openpnp/openpnp/pull/426
	https://github.com/openpnp/openpnp/issues/421
	
	Thank you @dzach!
	
# 2017-01-15

* ReferenceSlotAutoFeeder

	A new feeder called ReferenceSlotAutoFeeder has been added which provides the capability
	of a movable auto feeder.
	
	ReferenceSlotAutoFeeder allows you to define any number of feeder slots on your machine
	and each one can contain a feeder. By changing the feeder in a slot you move it's settings
	from slot to slot without having to reconfigure it's position, part or other settings.
	
	The primary purpose of this feeder is for machines that have auto feeders that can be readily
	moved from position to position.
	
	More information at https://github.com/openpnp/openpnp/wiki/ReferenceSlotAutoFeeder.

	Work was done in https://github.com/openpnp/openpnp/issues/399.

# 2017-01-09

* QR Code Based X Out Example Script

	A new example script called QrCodeXout.js is now included with OpenPnP. It will scan
	each board in a job for a QR code and if one is found the board will be disabled.
	This is an easy way to mark bad boards in a panel before starting a job.
	
	The purpose of the example script is to show how to use the QR code reader to
	do a complex task. It can easily be modified to search for other markings or
	other types of codes.

# 2017-01-06

* ScriptRun CvPipeline Stage

	A new CvPipeline stage has been added called ScriptRun. This stage take a file and
	runs it as a script with one of the supported built in script engines. This makes
	it very easy for people to add their own vision logic to a pipeline without having
	to write a stage.
	
	The script is supplied globals of `pipeline` and `stage`.
	
	An example script is shown below. It sets all of the pixels of the input image to
	the color green.
	
	```
	pipeline.workingImage.setTo(new Packages.org.opencv.core.Scalar(0, 255, 0));
	```
	
	By saving the above to a file with the extension .js and selecting it in the stage the
	script will run each time the stage is evaluated.
	
# 2016-12-30

* OpenCvCamera Capture Properties

	You can now set a number of capture properties on the OpenCvCamera. Not all properties are
	supported on every system or every camera. This feature is experimental and is primarily
	intended to allow users to experiment with exposure and format control.
	
	To set properties open the configuration for the camera, select the Camera Specific
	tab and look for the Properties section at the bottom.
	
	More information is available at: https://github.com/openpnp/openpnp/issues/328
	
# 2016-12-29

* Auto Feeder Improvements

	ReferenceAutoFeeder can now use actuators in boolean or double mode and the configuration
	panel has been updated to support each.
	
	ReferenceAutoFeeder also now supports a post pick actuator, which is helpful to support
	feeders that require two movements for a single feed operation. This is common in the
	Yamaha CL feeders that are becoming popular. The feed actuator is used to retract the guard
	and the post pick actuator is used to advance the tape after a pick.
	
	Existing feeders should not require any changes to support these new features. The defaults
	have been maintained.
	
* Post Pick Supported in Feeder Panel

	The post pick operation is now called from the Feeders tab when you run a Pick operation. This
	is useful when testing feeders that use this feature. Prior to this addition the post pick
	operation was only called when a job was running.

* Help Menu

	There is now a new Help menu that has quick links to important documentation and a new
	option to let you check for updates to OpenPnP. This option is only enabled if you
	installed OpenPnP with the installer. It will not be available if you built it from
	source.

* Camera Improvements

	ReferenceCamera is now much smarter about handling problems with invalid images and it should
	no longer cause high CPU usage when a camera configuration is incorrect. In addition, when
	it receives a null image it will retry up to 10 times before failing. This is helpful for
	cameras that sometimes return bad images; common with the ELP series of USB cameras.

* DipTrace Native Import

	With many thanks to @BendRocks, we now have native DipTrace import support. Find the new
	importer under the File -> Import menu.

# 2016-12-20

* User Interface Improvements

	This change introduces a new layout and some changes to the main user interface. The purpose
	of this change is to improve use of screen real estate for the things that people spend the
	most time interacting with.
	
	The main change is that the screen is now split vertically instead of horizontally. Cameras
	have been moved to the upper left and jog controls to the lower left. The tabs and tables have
	been moved to the right side of the screen and are now stacked rather than side by side.
	
	The primary benefit of this change is that it takes better advantage of the trend towards wider
	screens. Previously, users with wide (but short) screens had very limited space to work in
	due to the fixed sizes of several components. With the screen now split vertically it is
	now possible to use the full height of the screen to see jobs, placements and configuration
	information.
	
	Other minor changes are:
	* You can now collapse the jog controls to get them out of the way if you prefer to use the
	keyboard shortcuts or camera jogging.
	* DROs have been moved from the Machine Controls section to the bottom right of the status
	bar. For too long the DROs have taken up a huge amount of screen space for something that
	is really not that useful. You can still switch between relative and absolute mode by clicking
	on them.
	
	Here are some screenshots to show the major differences. The first shows the interface before
	the changes, the next shows the new user interface with jog controls expanded and the third
	shows the new interface with jog controls collapsed.
	
	![screen shot 2016-12-20 at 5 53 55 pm](https://cloud.githubusercontent.com/assets/1182323/21372675/562c4ae6-c6de-11e6-8071-86b126f78b95.png)

	![screen shot 2016-12-20 at 5 37 16 pm](https://cloud.githubusercontent.com/assets/1182323/21372503/1c1ac8f6-c6dd-11e6-89ee-64e922fbcdcf.png)

	![screen shot 2016-12-20 at 5 40 57 pm](https://cloud.githubusercontent.com/assets/1182323/21372502/1c11244a-c6dd-11e6-9f4a-fb37ba47c5e0.png)
	
	Finally, I would like to send a special Thank You to @FinalPhoenix, who has generously
	volunteered to help with improving OpenPnP's user interface and overall user experience!
	
	Many of these changes were suggested by @FinalPhoenix and with her help I hope to make
	OpenPnP far more enjoyable and easy to use than it has ever been.
	 
# 2016-12-19

* Introduction of Navigation Panel

	The Navigation Panel is a new feature that shows a 2D rendered view of your machine and job
	and allows quick navigation around the various components. You can quickly jog to any location
	on the machine and you can see a live view of your cameras, nozzles, feeders, boards and
	placements. Additionally, it makes it very easy to quickly find these objects in the tables
	below by simply clicking on one of them in the panel.
	
	This feature is still under heavy development. There are some known bugs and limitations:
	* When an item is selected, the selection border rendering sometimes gets artifacts from
	the drag jog line.
	* Board bottoms are not rendered correctly.
	
	For more information about this feature, see https://github.com/openpnp/openpnp/issues/99.
	
	Some things to try are:
	* Load a job to see a visual representation of the boards and placements.
	* Use your mouse wheel to zoom in and out.
	* Mouse over objects to see their names.
	* Click on various objects to select them and to instantly navigate to them
	in the tables below.
	* Click and drag to jog the camera around.
	* Click on the camera to turn it transparent so you can see what is under it. 
	
# 2016-12-18

* Add and Remove Cameras, Nozzles, Nozzle Tips, and Actuators in the Machine Setup tab.

	You can now add and remove cameras, nozzles and actuators in the Machine Setup tab. To add,
	click on the heading for the thing you want to create and click the Add button above. To
	remove, click on the one you want to remove and then click on the Remove button above.

* Cameras Tab Removed

	The Cameras tab has been removed and all of it's functionality moved to Machine Setup. This
	is part of a long running change to move all setup tasks into Machine Setup, and this is the
	final one.
	
	More information at: https://github.com/openpnp/openpnp/issues/103
	
# 2016-12-11

* Camera Drag Jogging

	You can now click and drag in the camera view to move the camera. When you click and begin
	to drag, a white line is shown from the current center of the camera to where the camera
	will go when you release the button. This is in addition to the existing Shift-Click to
	jog system already in place. It is intended to eventually replace that system.

* Navigation View Updates

	Navigation View is a feature that has been in development for some time but has been disabled
	in the code. It presents a 2D rendered view of the machine from the top down and allows
	you to quickly move around the machine and get information about objects such as boards,
	placements, feeders, etc. This feature is still under development and not ready for prime
	time, but it's become interesting enough that some people may want to try it out. To enable
	it you need to add -DenableNav=true to your command line.
	
	Once enabled, there will be a new tab with the Cameras call Navigation. Try loading a job
	to see what the view shows. You should see boards, placements, feeders, cameras and a red
	dot for your nozzles. You can zoom in and out with the mouse wheel, mouse over objects to
	get information, click and drag to jog the machine, and click cameras to turn them
	transparent. Cameras move and update in real time.
	
	This feature is going to grow quite a bit in the coming months. This is just a preview.
	
* Event Bus (Developers)

	A simple event bus has been added at Configuration.getBus(). This is currently being tested
	with the Navigation View above and is intended to further decouple the UI. This new addition
	is being tested for further use. For more information, see:
	
	https://github.com/google/guava/wiki/EventBusExplained

# 2016-11-26

* Scripting Events

	Scripting Events is a new feature that will be getting a lot of use in the future. This allows
	scripts to be run when certain things happen in OpenPnP. The scripts are referenced by name
	and can be of any supported scripting extension. They are found in the scripts/Events
	directory.
	
	The feature is used by calling `Scripting.on(String eventName, Map<String, Object> globals)`.
	
	As part of this feature, Scripting was moved into Configuration rather than MainFrame, so
	that it can be used outside of the UI.
	
	For more information, see https://github.com/openpnp/openpnp/wiki/Scripting#scripting-events.
	
* Camera.BeforeCapture and Camera.AfterCapture Scripting Events

	The first use of the new feature described above is two events that can be used to control
	lighting and other complex camera operations. Camera.BeforeCapture is fired before an image
	is captured and Camera.AfterCapture is fired after the capture is complete.
	
	By using the scripting events in combination with named Actuators you can control any
	device on your machine.

	For more information, see https://github.com/openpnp/openpnp/wiki/Scripting#camerabeforecapture
	and https://github.com/openpnp/openpnp/wiki/Scripting#cameraaftercapture

* Removed Deprecated LtiCivilCamera and VfwCamera

	LtiCivilCamera and VfwCamera were camera implementations for Mac and Windows. These required
	native libraries that were out of date and, as far as I know, no longer used. These have been
	removed in an effort to remove dead code from the project.
	
	If you were depending on these, please try switching to OpencvCamera or WebcamCamera. See
	https://groups.google.com/forum/#!msg/openpnp/JnOMjZWi9C8 for more information.

# 2016-11-17

## Note: Breaking Change

The Pick and Place Vacuum Sensing feature was originally released with different configuration
variable names. If you installed this version which was released yesterday then your configuration
will fail to load when you install this version. To fix it, edit your machine.xml and remove the
lines that include `vacuum-level-min` and `vacuum-level-max`.

## Changes

* Pick and Place Vacuum Sensing

	You can now set a "part on" and "part off" vacuum level on your nozzle tips. The values will
	be checked during the pick and place operations. Currently only supported in GcodeDriver.
	See https://github.com/openpnp/openpnp/wiki/GcodeDriver#vacuum_report_regex and
	https://github.com/openpnp/openpnp/wiki/GcodeDriver%3A-Command-Reference#vacuum_request_command
	for more information.
	
	Thank you to Matt Brocklehurst for this feature.
	
* Camera View Zoom

	You can now zoom into the camera view using the mouse wheel. The zoom level does not affect
	what the software sees when taking a picture, it only affects what you see. This can be used
	to make it easier to pinpoint locations when doing setup work.

* Feeder Post Pick Event

	Feeders now support a post pick event which allows for actuation after the pick is complete.
	This feature is primarily for custom feeders and it is not currently used in the default
	feeders.

	Thank you to Matt Brocklehurst for this feature.

* GcodeDriver Controller Error Detection

	GcodeDriver can now detect error responses from the controller using the `COMMAND_ERROR_REGEX`.
	See https://github.com/openpnp/openpnp/wiki/GcodeDriver#command_error_regex for more details.
	
	Thank you to Matt Brocklehurst for this feature.

# 2016-10-28

* Nozzle Tip Changer Fourth Move Added

	A fourth move option has been added to the nozzle tip changer to support LitePlacer like
	hardware configurations. By default the fourth move is cloned from the previous second move
	upon configuration load, so the change should not result in any new moves for existing
	configurations.
	
	https://github.com/openpnp/openpnp/issues/354 

* Logging Changes

	OpenPnP now uses [TinyLog](http://www.tinylog.org/) for logging instead of SLF and Log4J.
	TinyLog is much simpler to configure and far less complex to work with. It has quite a few
	less features, but based on feedback in https://github.com/openpnp/openpnp/issues/333 this
	was preferred.
	
	* Logging now defaults to the INFO level.
	* The level can be changed from the Log tab and it will be saved.
	* Log level can be changed on the fly without restarting OpenPnP.
	* Rotating log files are still created. The naming has changed slightly. The files now rotate
	after each startup instead of daily.
	
* Camera View Reticle Now Tracks Tool Rotation

	Prior to this change, the camera view reticle / crosshair always tracked the rotation of the
	camera. Since most cameras don't rotate, this didn't make much sense. The view now tracks the
	rotation of the currently selected tool, which is almost always the nozzle.
	
	https://github.com/openpnp/openpnp/issues/347
	
* Strip Feeder Improvements

	* Strip feeder now moves to the correct pick location before the first feed, i.e. when the
	feed count is 0. This is just a bit of user friendliness that does not really change any
	functionality. https://github.com/openpnp/openpnp/issues/352
	* Strip feeder auto setup no longer captures or overwrites the Z value.
	https://github.com/openpnp/openpnp/issues/353
	
# 2016-10-17

* GcodeDriver CamTransform

	GcodeDriver now supports cam based Z axes like those used on the OpenBuilds reference
	design, the RobotDigg head and several other common head designs.
	
	To use the new transform, see the example configuration below.
	
	```
    <axis name="z" type="Z" home-coordinate="0.0">
       <head-mountable-ids class="java.util.HashSet">
          <string>69edd567-df6c-495a-9b30-2fcbf5c9742f</string>
          <string>169edd567-df6c-495a-9b30-2fcbf5c9742f</string>
       </head-mountable-ids>
       <transform class="org.openpnp.machine.reference.driver.GcodeDriver$CamTransform" cam-radius="24.0" cam-wheel-radius="9.5" cam-wheel-gap="2.0">
          <negated-head-mountable-id>169edd567-df6c-495a-9b30-2fcbf5c9742f</negated-head-mountable-id>
       </transform>
    </axis>
	```
	
	In particular, you must define your Z axis head-mountable-ids to your two nozzles, and
	you must set the negated-head-mountable-id to the secondary nozzle. The parameters for
	defining the cam are:
	
	* cam-radius: The radius of the cam itself.
	* cam-wheel-radius: The radius of the bearings or wheels at the end of the cams that actually
	push the axis down.
	* cam-wheel-gap: The gap, if any, between the cam wheels and the top of the axis which they
	push down.	

# 2016-09-07

* Success and Error Sounds, Signaler Interface

	OpenPnP can now play sounds when a job finishes or fails due to error. This feature
	also introduces a Signaler interface which will be used in the future to allow
	for external hardware to be triggered for the same events.
	
	For more information on this new feature, see:
	https://github.com/openpnp/openpnp/wiki/Signalers
	
	Thank you to @pfried for contributing this feature!

# 2016-08-27

* GcodeDriver Gcode Configuration UI

	You can now configure all Gcode commands and RegExs via the driver configuration
	wizard found in Machine Setup -> Driver -> GcodeDriver. The wizard has two tabs:
	Serial and Gcode. In the Gcode tab you can choose the tool you want to configure
	and the command for that tool. By choosing the Default tool you configure the
	default set of commands which are used for fallbacks when tool specific commands
	are not found.

* GcodeDriver Commands Now In CDATA

	GcodeDriver commands are switched to use CDATA now, instead of escaped XML. This makes
	it easier to include complex regexs that may include XML characters. In general, you
	don't have to change anything. OpenPnP will update your config the first time you run
	it. The resulting commands look like:
	
	```
	<command type="CONNECT_COMMAND">
		<text><![CDATA[G21]]></text>
	    <text><![CDATA[G90]]></text>
	    <text><![CDATA[M82]]></text>
	</command>
	```
	
	Only the data between the [] is considered part of the command.
	
* GcodeDriver Position Reporting

	GcodeDriver will now read position reports from the controller. This can be used to
	provide feedback during moves or for controllers that may move externally to
	OpenPnP. This is a very new feature and is expected to require some iteration before
	it's perfect. If you run into issues with it, please report them.
	
	To add position reporting, define a new regex in the format of:
	
	```
	<command type="POSITION_REPORT_REGEX">
		<text><![CDATA[<Idle,MPos:(?<x>-?\d+\.\d+),(?<y>-?\d+\.\d+),(?<z>-?\d+\.\d+),(?<rotation>-?\d+\.\d+)>]]></text>
	</command>
	```
	
	Note that the regex contains named groups. The named groups are used to identify the
	coordinates of each axis you have defined. You should name the groups with the same
	names in your axes section. In the command above the groups / axes are named
	x, y, z and rotation.

# 2016-08-08

* GcodeDriver Tool Specific Commands

	GcodeDriver now has the ability to send different commands based on the the tool that
	that the command is being sent for. This means that you can have different commands for each 
	object on the head such as Nozzles, Cameras, Actuators, etc. Most importantly, you can now
	have separate pick and place commands for each Nozzle.
	
	When you first start OpenPnP with this version it will automatically update your
	configuration and move the existing commands into a default command set. After closing
	OpenPnP, please inspect your machine.xml to see the changes.
	
	To specify a specific tool for a command, the following syntax is used:
	```
     <command head-mountable-id="269edd567-df6c-495a-9b30-2fcbf5c9742f" type="PICK_COMMAND">
        <text>M808</text>
        <text>M800</text>
     </command>
     <command head-mountable-id="69edd567-df6c-495a-9b30-2fcbf5c9742f" type="PICK_COMMAND">
        <text>M808</text>
        <text>M802--</text>
     </command>
	```
	
	Note that the PICK_COMMAND is specified twice. One for each nozzle. The head-mountable-id
	specifies which nozzle the command is for.
	
	OpenPnP will first search for a command that matches the specified tool, and if it cannot
	find one for the tool then it will default to the command defined without a head-mountable-id.
	
	The commands that support tool specific codes are:
    * MOVE_TO_COMMAND
    * PICK_COMMAND
    * PLACE_COMMAND
    * ACTUATE_BOOLEAN_COMMAND
    * ACTUATE_DOUBLE_COMMAND

# 2016-06-22

* Python Scripting Support

	Python support is now included by default, instead of requiring an external install.

* GcodeDriver Pump On, Pump Off

	GcodeDriver now has pump-on-command and pump-off-command commands which will trigger
	intelligently depending on whether there are any nozzles currently picking. 

# 2016-06-21

* GcodeDriver Axis Mapping

	The GcodeDriver now has a system for mapping axes to object on the head, along with a
	system for transforming coordinates on each axis. This allows more complex head setups than
	the basic single nozzle, four axis setup. In particular, this system allows for the case
	where a single Z motor powers two Z axes either in a cam, belt or rack and pinion
	configuration by specifying a single Z axis with two nozzles mapped to it, along with an
	appropriate transform. See https://github.com/openpnp/openpnp/wiki/GcodeDriver#axis-mapping
	for more information.
	
# 2016-06-20

* Scripting Engine

	OpenPnP now has the ability to run user provided scripts that have full access to the
	OpenPnP API and GUI. This makes it easy to add new utilities and functionality to
	your installtion of OpenPnP without having to modify the code.
	
	For more information, see:
	https://github.com/openpnp/openpnp/wiki/Scripting

# 2016-06-19

* ONVIF Camera Support

	Thanks to @richard-sim we now have support for IP cameras using the ONVIF standard. This
	standard is used by many IP cameras, especially in the realm of security cameras. This brings
	cheap IP camera support to OpenPnP and opens up the options for cameras much wider than before.

# 2016-06-16

* Log Tab

	There is a new main window tab called Log that shows logging output. This makes it easier for
	you to see the output of various commands in OpenPnP. It has options to limit the length of
	the log shown, and the log level. This is the first version of the feature and does not
	include all of the features that are planned. More information is available at:
	https://github.com/openpnp/openpnp/issues/288

# 2016-06-03

* GcodeDriver Move To Complete Regex

	You can now include <move-to-complete-regex> in your GcodeDriver configuration to specify
	a regex that the move-to command will wait for before completing. This is used for motion
	controllers that return the command confirmation before movement is complete - TinyG
	in particular. See https://github.com/openpnp/openpnp/wiki/GcodeDriver#move-to-complete-regex
	for more information.

# 2016-05-25

* OpenBuilds Driver Rotation Improvements

	The OpenBuilds Driver now treats the rotary axes as rotary axes, instead of linear ones. This
	means that it will choose to turn the opposite direction if that is the faster way to reach
	a given position. In other words, if you are trying to move from 355 degrees to 10 degrees
	it will counterclockwise 15 degrees, passing through 360 degrees instead of clockwise 345
	degrees passing through 180 degrees.
	
	This greatly improves performance related to the recent change to treat all rotation moves
	as solo. 

# 2016-05-16

* Nozzle Park

	https://github.com/openpnp/openpnp/issues/76
	
	* There is now a per head park location. You can set the location in Machine Setup -> Heads.
	
	* The Zero buttons in the jog controls panel have been replaced with Park buttons. Each will
	move the selected nozzle to the park location.
	
	* The Job Processor will now optionally park the nozzle after a job completes, instead of
	re-homing. You can turn this option on in Machine Setup -> Job Processors.

# 2016-05-15

* Multi-Select on Parts and Packages Panels

	You can now select multiple entries in the Parts and Packages panels for deletion. 

# 2016-05-14

* Job Processor Refactor

	https://github.com/openpnp/openpnp/issues/265
	
	This is a major rewrite of the JobProcessor, which is the code the handles the actual
	running of jobs. The purpose of this rewrite is to address many issues that have cropped
	up over the years in the JobProcessor, primarily around error handling and retry. The
	following changes are included:
	
	* Feed Retry: You can specify a retry count (default 3) in the feeder configuration and if a 
	feed operation fails on a given feeder the operation will retry that many times before
	disabling the feeder and advancing to the next available one. If no more feeders are available
	for the part the feed operation fails and the user is notified.
	https://github.com/openpnp/openpnp/issues/206
	
	* Job Error Recovery: If any part of a job fails the user is now presented a dialog offering
	the options Retry, Skip, Pause to resolve it. No more "The job will be paused"
	leaving the system broken. Retry will attempt to re-run the previous task. Skip will skip
	processing the current placement. Pause pauses the job and hides the dialog so that the
	user can make configuration changes to attempt to resolve the error. The job may be
	continued with the Run or Step button.
	
	* Home After Job Complete: When a job completes normally the machine will return to home.
	https://github.com/openpnp/openpnp/issues/76
	
	* Pre and Post Job Machine Cleanup: Before a job starts and after it either finishes or is
	aborted, if any nozzles are holding a part the part will be discarded.
	https://github.com/openpnp/openpnp/issues/102
	
	
	Note to developers: The code and API for this feature is considered alpha quality and is
	expected to change. I am not happy with the code quality of the feature, but I am very happy
	with the functionality, so I am releasing it. I intend to revisit this and make significant
	changes to both the code and the API.
	
* Solder Paste Dispense Temporarily Disabled

	Due to the Job Processor Refactor above, Solder Paste Dispense is temporarily disabled. Please
	see https://github.com/openpnp/openpnp/issues/271 for more information.

# 2016-05-12

* Show Camera Names in All Camera View

	When the "All Cameras" view is selected, the name of each camera will be shown in in a little
	box in the bottom left of the view. This makes it easier to know what you are looking at when
	you have multiple cameras in action.
	
* FPS Limit Option in OpenCvCamera

	You can now set an FPS limit in the OpenCvCamera wizard. The default is 24. This is helpful
	to limit CPU usage on a machine with high resolution cameras.
	
* Removed Bottom Vision API from VisionProvider

	Before the Bottom Vision feature was complete it had been stubbed into the VisionProvider
	API. Since Bottom Vision is it's own first level object now this is no longer needed, so
	it has been removed. Existing implementations should move to either ReferenceBottomVision
	or to their own specific implementation of PartAlignment.
	
* Camera Crop

	https://github.com/openpnp/openpnp/issues/171

	You can now set a crop width and height on your camera. The crop is applied from the center,
	so setting a crop of 200x200 will make the output from the camera only the center 200x200
	pixels. This is useful for when you have a high resolution camera but only care about a
	small portion of it. Cropping decreases the amount of data that is required to be processed
	and cuts down CPU and memory usage.
	
# 2016-04-27

* Speed Values Normalized

	Anywhere in the UI referring to a speed is now expressed as a percentage. This includes
	the new speed limit slider, parts speed and drag feeder speed.

* Global Speed Limit

	There is now a slider in the jog controls that controls the overall speed of the machine
	from 0 - 100%. The speed is applied to all other speeds in the system. Specifically, if
	you have a part specific speed the speed applied to that part will be the global speed
	times the part speed.

# 2016-04-24

* Bottom Vision!

	This is the first release of the Bottom Vision feature. This is a feature that has been
	in development for quite a long time and along the way has picked up a number of small
	but important features to go along with it.
	
	More information about Bottom Vision can be found at:
	https://github.com/openpnp/openpnp/wiki/Bottom-Vision
	 

# 2016-04-23

* Moved the jog controls from their dialog to the main window.

	The jog controls are now always available and have been modified to take up less vertical
	space. The large START / STOP button has been replaced with a smaller "Power" button
	in the jog controls and the jog increment slider has been changed to vertical instead of
	horizontal and also integrated into the jog controls.
	
	The end result is that the jog controls now take up about the same amount of space as the
	big START button + increment slider used to except now they are visible all the time. 

* Changed Nozzle.pick() to Nozzle.pick(Part) and added Nozzle.getPart()

    This change allows the Nozzle to be aware of what Part it has picked and also allows callers
    to find out the same. This is used in SimulatedUpCamera to render the Part, will be used in
    the Nav View to render parts, is used in controlling movement speed based on Part speed
    and will be used in future improvements to the part discard bin.
    
* Part speed is now enforced in Nozzle.moveTo instead of randomly all over the program.

	This is possible due to the Nozzle.pick(Part) change above and improves the overall
	code quality related to part speed.

* Add a Discard Location to machine.

	Discard location can be configured in Machine Settings -> Machine. The Discard location
	is used as a dumping area for parts when an error occurs.
	
* Add Discard button to Special Commands window.

	Allows you to manually discard any part that is currently picked on the selected nozzle.

* Add Pick button to Parts panel.

	The Pick button allows you to pick the selected part from the first available feeder.
	
* Remove Pick and Place buttons from Special Commands window.

	This functionality was misnamed in that it did not actually pick or place a part, it was just
	used to turn vacuum on and off. Pick functionality is now handled by the Pick buttons on the
	Parts and Feeders panels and Place functionality is handled by the Discard button.
	 
