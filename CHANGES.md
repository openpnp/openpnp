This file lists major or notable changes to OpenPnP in chronological order. This is not
a complete change list, only those that may directly interest or affect users.

# 2016-07-16

* GcodeDriver Command Sets

	GcodeDriver now has command sets that can be applied to a specific tool. This means
	that you can have different commands for each object on the head such as Nozzles,
	Cameras, Actuators, etc. Most importantly, you can now have separate pick and place
	commands for each Nozzle.
	
	When you first start OpenPnP with this version it will automatically update your
	configuration and move the existing commands into a default command set.

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
	 