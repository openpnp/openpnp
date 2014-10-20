Here are some thoughts and ideas on how the OpenPnP UI will be redesigned in the near future.

# September 7, 2014

The stories below are a good start on UI rework, and good thinking. But part of this is whether or not it fits in the world of FPD.

So, does it?



# September 5, 2014

Let's work on user stories.

# I can run a job placing one or more boards.
	* I fix one or more boards to the machine.
	* I press the Go button.
	* OpenPnP automatically identifies and orients the boards.
	* I am prompted to manually identify or set up any unidentified boards.
	* OpenPnP determines the parts required to complete the job.
	* OpenPnP determines if the required parts are available in loaded feeders.
	* I am prompted to load feeders with any missing parts.
	* OpenPnP begins placing parts.
	* I am prompted if there is an error.

## OpenPnP can find, identify and orient boards using a camera.
	* OpenPnP analyzes it's database of board definitions and builds fingerprints to search for.
		* Board definitions contain information about unique marks on the boards such as fiducials, codes, etc.
	* OpenPnP uses the camera to search the extents of the machine for matching board fingerprints.
		* It may also use edge finding to find boards to limit the search.
	* OpenPnP determines the definition, quantity, side and orientation of each board that is to be placed.
	* OpenPnP shows the results of the identification step as a list of boards and orientations.

## I can manually identify an unidentified board.
	* This is dependent on the reason that OpenPnP cannot identify the board. Some considerations:
		* The data is good but the vision system is failing for some reason.
		* The board definition is unknown so OpenPnP cannot identify the board.
		* The board has multiple possible board matches so one must be chosen.
	* If I load a new board file OpenPnP can recheck to see if it can identify using that board.
	* I can force OpenPnP to use a given board definition for a board. This may require a manual orientation step as well.
	* If no resolution can be found I can move on to set up a new board.

## I can set up a new board.
	* I can either begin an import process or create a new board definition document.
	* I can add placements, which consist of:
		* Reference ID (e.g. R1, C2, U3)
		* Part definition (e.g. C-0805-104k, R-0603-10k)
		* Board side (e.g. Top, Bottom)
		* X, Y, and Theta coordinates 
	* I can add identifiers, such as:
		* Fiducials, which consist of:
			* Type
			* Shape
			* Size Information
		* QR Codes
		* Barcodes
		* Pads as fiducials
			* When fiducials are not available, these can be used as a substitute
	* I can edit properties of the board definition such as it's name, description, copyright, etc.

## I can load a feeder with a specific part.
	* Choose from a list of installed feeders, or create a new feeder.
	* Choose the part that is loaded into the feeder.
	* Specify quantity of parts available to the feeder if possible.


# I can create a new feeder.
	


# New Ideas March 1, 2014

  

* Automate all the things! 

  

How about this for a workflow?

  

Fix some boards to the table. Usually you will only be placing the same type
of board in a run.

Hit go.

Machine uses camera to find the boards, find the fiducials and determine what
boards are being placed.

     If the machine doesn’t know the board, start a board setup process.

When boards are identified determine if the correct feeders, nozzles, other
support stuff are available.

     If not, prompt for that stuff. (See Go Panel below)

     For feeders, can use internal saved config or just scan QR codes each
time. 

          If we can’t find what we need, ask the user to install what’s needed
and identify it by QR code.

Place that shit.

  

## Go Panel (Ready List?)

  

I like the idea of showing the user a “Go Panel” showing what is not ready and
ways to fix it. For instance, if feeders are missing, each one would have a
line in a table with a button to configure the feeder, or install it or
whatever.

  

By putting this information in the Go Panel we get rid of all tabs and config
sections and just give the user things they can act on.

  

The program could even start out totally unconfigured and guide the user
through the setup process in this fashion.

  

# Workflow

  

# Task Rarity

  

## Rare

  

* Cameras

* Drivers

* Heads

* Nozzles

* Actuators

  

## Semi-Rare

  

* Packages (we actually want this to be Rare, once we have them all it will be)

* NozzleTips (may be Frequent if manually switching)

  

## Common

  

* Parts

* Boards

  

## Frequent

  

* Jobs

* Board orientation

* Feeders

  

# General Changes

  

* Get rid of the Boards tab entirely. Editing a board while a job is running is basically stupid. You won't have access to the machine or the feeders or anything useful. Boards can be set up in jobs.

  

* Alternately, maybe the job format is dumb. Maybe listing the board more than once is a confusing concept. Instead, list each board and the detail table can be a list of board locations. If you want to edit the board you double click it and we go to the board editor. Or maybe the left side is a tree of Job->Board->BoardLocation. If you have the board selected, the right side is placements. If you select the BoardLocation the right side is a wizard specially for setting board location.

  

* Another alternately: Maybe the job itself is dumb. Do you ever want to place different boards during a session? Probably not. It would typically mean using different feeders, nozzles, etc. Shit.

  

# Flows

  

## Place a single board

  

The user wishes to place a single board, i.e. to build a prototype.

  

1. Secure the board into the machine.

2. Open the board file.

     If a board file has not been created, see the "Create a board file"
flows.

3. 

  

## First time setup

  

Primarily based around machine setup. Encompasses a lot of the "Rare" tasks.

  

Machine setup can probably be moved into it's own window, but we need to
facilitate things that require messing around with the machine. This may mean
showing camera views where needed, allowing measurements, showing reticles,
jog controls, picking current nozzle, etc. Much of the stuff we can do from
the main screen right now will be required for machine setup.

  

Avoid a situation where the user is trying to setup or run a job and they ever
have to open the machine window.

  

  
# Neil's Comments and stuff

Note!  Some of this may be duplicating what's above.  I'm just putting it all here for now as a brain dump, we can get it all sorted out later.

## FireSight Integration

I will be spending the first big chunk of my time integrating https://github.com/firepick1/FireSight/wiki into OpenPnP via "FireSightVisionProvider.java".  

* This will allow a pipeline of CV stages to be processed on an input image, and will return JSON results, and an output image.  
* I'm going to make a vision test panel for debugging the various stages, this will allow rapid development and real-world testing of CV code without having to run a job or create java tests.

### High level CV stuff that will be integrated into OpenPnP:

* get pixels-per-mm (automatically finds the pixels per millimeter from a piece of 8mm white paper SMT tape)
* Camera Calibration (lens distortion, perspective, correction, etc..)
* MatchTemplate
* HoleFinder / HoughCircles
* CalcOffset (used for cal, repeatability, etc)
* WarpAffine (rotation, scaling, etc..)
* putText (for on-screen display of relevant info)
* QRDecode (for recognizing feeders and other stuff)
* Image stitching (for doing bed scans and stitching them into a single image)

These functions will be stringed together to allow cameras to be mounted in any direction and angle, and have the CV-corrected output fed into other 

## UI improvements

### Separate OpenPnP into task-based chunks, each with their own panel.  

Some sort of front "home" screen would allow you to get to each of the tasks.  Ideally each would be a nice big icon, color coded with PNG/SVG graphics or whatnot.  

* Panel: CAD file import.  Making this a full panel would eventually allow you to see a preview of your job as you import it, and make changes before saving.
* Panel: Pick and place job.  This would allow you to create, load, and save jobs and run them.  This is the screen that would have the cameras, jobs, RUN/STOP, etc... that would be used when actually in production.  I'm not sure if jobs and boards should be separate panels.  I'm leaning towards 'yes' as long as you can switch between them during a job.
* Panel: Machine Config (machine.xml editing via GUI panel).  The way you're implementing it now with an XML is perfect.
* Panel: Machine XYZ calibration by vision (this may be a FirePick only thing, on our fork, that we take full responsibility for implementing and maintaining)
* Panel: Feeder setup (this panel would allow you to auto-find and configure feeder locations, pick spots, status, and other important bits).  "Feed" buttons for each feeder should be clearly marked and accessible.
* Panel: Package/footprint editor.  Gives users the ability to create a footprint 
* Panel: Paste dispense.  May be FirePick only at first (again, I'll be implementing this on my own, in my own fork), but others have expressed interest in having it. 
* Panel: 3D printing.  FirePick only, I know you want no part in this :)  We'll keep this on our fork, but obviously if we're just inserting panels that call code into the main "home" screen, then adding this doesn't break anything in OpenPnP.  It's actually pretty simple if you think about it, we're just loading a job, sending Gcode, and monitoring toolpaths and temperatures.  The coolest part is that we can plot the temperature to a nice stripchart in PNG format, and "add" it as a camera in the machine.xml.  It actually integrates into OpenPnP cleanly inside our fork, and doesn't require anything to be sent upstream to you.
* Panel: Tool setup.  This is a FirePick only thing.  our machine has four modular tools.  If the whole task/panel thing happens, it's easy for us to add this in our fork, and would make it extremely intuitive for people to change tools on our machine.  It's basically a dumbed down way of configuring and displaying tools to the user.
* Panel: Vision debug.  This is debug only, and can be hidden completely from the GUI if needed.  it just makes it really easy to debug vision and so forth from the GUI.  This is really just a development time saver.
* Panel: Motion debug.  This is something that almost every single other CNC Control program has to some extent.  Basically take all the text going out to the serial port, and show it in an autoscrolling text box with a max buffer size.  Allow the user to type in text at the bottom to send stuff to the motion controller.  
* Panel: Optical inspection.  This is something that should appeal to anyone with a Pick and Place machine, and there's no reason why we can't attempt to use the camera to do rudimentary optical inspection with a downward looking camera, to check for solder paste defects and SMT component defects.  This is something that FirePick really wants to support down the road.  Not sure when it will happen but we definitely want to do it.
* Panel: Conveyor setup.  Not implemented any time soon but would give us a place to do it when the time comes.  Most conveyors give you the option to pick the load and unload speeds, the board X, Y, and Z dimensions, and other offsets... Also buttons to load, unload, clamp, and unclamp.  Which can be called during a job if a particular machine has written that code.  But this would allow you to do all that in a debug/setup sort of fashion.
* Panel: Statistics and/or inventory.  Also gives the possibility of ERP integration (OpenERP / odoo, et. al.) to keep track of parts inventory.  Again, it's open source so if the project picks up steam, there's the possibility of someone else coming along and implementing it, etc.. This is a feature that all the big machines have, which would make a great use case for retrofitting really big old machines with OpenPnP and using in a high production environment.  Companies would likely pay a lot of money for this, even if the software is F/OSS. 
* Panel: Help.  Ideally would load a browser window or RichTextBox and would list out good help info that would help a user learn the workflow of an SMT job.  This would be just really high level help on workflow and order of operations.

So the beauty of all this is, it's easy to "lock out" or "hide" any of these panels and just display what a particular user wants on the home screen.  They can be created and deleted with relative ease, you just add in a statement or two to call the constructors in the MainFrame.java.  All the backend code can be kept in completely separate bundles, even in different org's in the case of the FirePick specific stuff above.

And obviously it doesn't mean that anyone has to write all those new pieces right away.  But by going with this paradigm, it would allow us to add them as the demand for them grew or as more people joined us to help code.  And by us, that could be people helping FirePick or people helping OpenPnP.  I do know that if FirePick is as successful as I'm hoping, there will be hundreds (maybe thousands) of folks out there that will want to start expanding it to do new awesome things. 

There's one other really cool use case that comes out the above task-based approach.  Currently, OpenPnP bombs/errors out when it's executed, if machine.xml is misconfigured.  Ideally, upon startup, all the settings would be loaded and made available for the Machine Config panel, even if there are errors, it would do its best.  The user can edit them at this point from the GUI to get them correct.  The constructors to the actual machine equipment have NOT been called yet.  They do not get called until the OpenPnP SMT job panel is opened (or any other panel that needs to actually move the machine or deal with cameras, etc..).  Therefore, a simple check can be made as to whether the machine.xml is currently valid.  If it is, then the panel is allowed to load.  If not, then a dialog will come up, giving the user the choice to either cancel back to the Home screen, or to click a button to jump over to the config and edit stuff until it's valid.

Also, instead of putting the motion controller COM port, baud rate, etc... buried in an XML or in a machine config page, they should probably be right on the main home screen.  It's something that happens to change a lot, especially in Windows, if you plug into a different port or USB hub.  Also I would have it where it allows you to connect and disconnect via a button on the GUI, rather than having it try to connect on program load like it does now.  This is a big cause of frustration for a good handful of people that I've talked to, that have used OpenPnP.

### Usability and Eye Candy

* Show simulated PCB(s), SMT components, feeders, tape, bed, current position, etc.. in a nice graphic.  Bonus points if it's zoomable.  Triple points if it's clickable to move the head to that position.  For now, this can just be implemented as a camera with a custom driver that displays the generated image instead of the camera.
* Show stitched image of the entire worksurface.  Similar to above.  Can be implemented as a camera with special driver.  This is similar to the current tablescanner, but this version would shrink the whole thing down to fit the screen, and overlay current position and other useful info on top of it.  The actual stitching happens in OpenCV, and we can use FireSight to generate the cursors on top of it.
* Remove the current machine jogging panel (the popout dialog one), and replace it with a jogging panel similar to [Printrun](http://reprap.org/mediawiki/images/f/f2/Printrun_fr.png) and/or [Simplify3D](http://www.simplify3d.com/wp-content/uploads/2014/01/Machine-Control-Panel.png).  These are much easier for jogging around than what's currently implemented.  Also notice how on both of those panels, they give you GUI elements for COM port, Baud rate, connect/disconnect, etc... This is MUCH more user friendly than the way OpenPnP currently handles it.
* "It is extremely useful to be able to stop a job, tweak vision parameters for the part being picked and carry on." - told to me by a SMT assembly veteran
* "small SO parts can come in tape or tube, and you may not  know which at design time, so you need to be able to override different feeder orientations seperate from the part definition. Similarly sometimes the same part (same to the extent of using the same part definition) may come on tape spaced at one to two indexes, so this also needs to be overrideable." - Also told to me by the same guy.
* "mis-picks & picks from empty slots are much more common than drops - you may want different behaviour though - mis-pick=retry, drop during travel = stop & alert user" - more tribal knowledge
* Feeder code should auto adjust its pick position based on offsets seen when visioning previous parts so it homes in on the nominal position.  Not sure if it currently does this or not but this is useful on certain parts that shift a bit in their carrier tape, especially SOT-23's and smaller parts.
* Vacuum sensing - We'll be adding this to the nozzles on our firepick fork.. we will likely send a pull request for it later on.. we're in no hurry to implement it but wanted to have it documented somewhere.
* "That is why you do vacuum integrity check. nozzle down, vacuum on , wait for detectable drop to a preset value ( pump down of the line. if the pick is half then you will never meet the vacuum threshold : eject part ( head travels to the 'bucket' where it drops the part ) and then does a retry. 3 retries : alert operator.  Even if you don't install vacuum sensor by default make sure we have access to a spare analogue input and can set a threshold on that value after pick. Pick part , wait for threshold ( with timeout ) then go do the place. retry 3 times."  Other guy said: "3 retries - give up on **that** part and get on with the next one, until it has nothing more it can do, **then** alert operator! "
* more tribal knowledge.  Q:What have you found to be the root causes for mis-picks from empty slots? A: Usually, running out of parts, obviously this gets caught later with vision.  Also tape cover not peeling, or splitting.  Mispicks can happen because a part simply vibrated out of its pocket. It can be clinging to the peeled tape, vibration could have cause it to stand up or pop out altogether.
