This file lists major or notable changes to OpenPnP in chronological order. This is not
a complete change list, only those that may directly interest or affect users.

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
	 