This file lists major or notable changes to OpenPnP in chronological order. This is not
a complete change list, only those that may directly interest or affect users.

# 2016-11-26

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
	 
