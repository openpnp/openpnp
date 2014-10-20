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

  

  

  

