This file lists major or notable changes to OpenPnP in chronological order. This is not
a complete change list, only those that may directly interest or affect users.

# 2017-04-13

* BREAKING CHANGE: Outdated Drivers Removed

	Several outdated drivers have been removed. These are: GrblDriver, MarlinDriver, SprinterDriver
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
	 
