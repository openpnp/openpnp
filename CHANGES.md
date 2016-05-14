This file lists major or notable changes to OpenPnP in chronological order. This is not
a complete change list, only those that may directly interest or affect users.

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
	 