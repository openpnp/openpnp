Introduction
============

OpenPnP is an Open Source SMT pick and place system designed and built with the hobbyist in mind but with the features and power to run commercial pick and place operations. It's goal is to bring pick and place to the desktop of anyone who needs to make more than a few of something.

OpenPnP is made up of three components. The hardware, the firmware and the software. This User Manual focuses only on the software. To get information about the hardware and firmware please visit http://openpnp.org.

The purpose of this manual is to help you get the software up and running, and to teach you how to configure and operate it.

Installation
============

Prerequisites
-------------

OpenPnP runs on the Java platform and requires the Java runtime version 7 or higher to run. You can download the latest version of Java from http://java.com/getjava.

OpenPnP is designed to run on Mac, Windows and Linux. Other platforms may be supported due to the nature of Java, but they are not recommended. OpenPnp is written and tested on Mac and used regularly on Windows. These two platforms are recommended for the best compatibility.

Download
--------

Visit http://openpnp.org/downloads to find out how to download the latest snapshot or release of OpenPnP.

Install and Run
---------------

If you are using one of the binary installers from the website, just run the installer and follow the instructions. After installation you can run OpenPnP from your operating system's applications list, i.e. Start Menu, Applications folder, etc.

If you are using an archive version of OpenPnP, unzip the software into a directory of your choosing. Typically this would be the same place you keep your other applications. Inside the folder you unzipped OpenPnP to there is an `openpnp.sh` and `openpnp.bat` script. These should work for Windows, Mac and Linux. For Mac and Linux, run `openpnp.sh` and for Windows run `openpnp.bat`. After a short wait you should see the OpenPnP Main Window. If something goes wrong, visit the Troubleshooting section of this document for help.

Coordinate System
=================

OpenPnP uses the right handed coordinate system which is also used in physics, math, 3D graphics and many CAD packages.

In this coordinate system we are standing above the machine, looking down at it.

The X axis moves right and left. Right is positive.
The Y axis moves forward and back. Forward is positive.
The Z axis moves up and down. Up is positive.
The C, or rotation, axis rotates clockwise and counter-clockwise. Counter-clockwise is positive.

More information can be found at:
http://www.evl.uic.edu/ralph/508S98/coordinates.html
http://en.wikipedia.org/wiki/Right-hand_rule

The User Interface
==================

OpenPnP is primarily a single window interface, broken up into multiple sections. Those sections will be explained in detail below.

The Main Window
---------------
![Main Window](http://globedrop.com/wiki/_media/openpnp:mainwindow.png)

Machine Controls
----------------
![Machine Controls](https://globedrop.com/wiki/_media/openpnp:machinecontrols.png)

The Camera Panel
----------------
![Camera Window](https://globedrop.com/wiki/_media/openpnp:camerawindow.png)

The Tabs
--------
![The Tabs](https://globedrop.com/wiki/_media/openpnp:tabswindow.png)

Shortcuts
---------

There are a few important keyboard shortcuts that are critical to know to use OpenPnP. All of the shortcuts use Ctrl + another key. They are:

* Ctrl+Tab: Open the jog controls window which gives you visible buttons for most of the other shortcuts.

![Jog Controls](https://globedrop.com/wiki/_media/openpnp:jogcontrols.png)
* Ctrl+Arrow Key: Jog the currently selected Nozzle in X and Y. Up and Down arrows jog in Y and Left and Right arrows jog in X.
* Ctrl+/, Ctrl+': Jog the currently selected Nozzle down and up in Z.
* Ctrl+<, Ctrl+>: Rotate the currently selected Nozzle counter-clockwise and clockwise.
* Ctrl+Plus, Ctrl+Minus: Change the jog increment slider. This changes how far each jog key will move the Nozzle.
* Shift+Left Mouse Click: Hold Shift and left click the mouse anywhere in the camera view to move the camera to that position.

Setup
=====

The Driver
----------

In OpenPnP, the Driver is the part of the software that interfaces between OpenPnP and a particular type of machine. Typically this is just a small piece of code that translates OpenPnP commands into commands for a particular motion controller such as Smoothie, TinyG, Marlin, etc.

Before you can move your machine you have to select and set up your Driver. To do that:

1. Go to the Machine Setup tab and select the root node of the tree. On most setups it's called "ReferenceMachine". A setup panel will appear on the right.
2. In the setup panel, select the driver that most closely matches you motion controller or machine. Click apply.
*Note: For more information about specific drivers see the Driver Details section below.*.
3. OpenPnP will prompt you to restart the program, so do that.
4. After restarting OpenPnP go back to the Machine Setup tab and find the Driver you selected in the tree. It should be near the bottom, under the Driver branch. Select it and a setup panel will appear.
5. At this point you can configure the driver with parameters that are specific to your machine. For instance, most drivers that talk to the machine over the serial port will have a combo box to select the port and baud rate. Hit Apply once you've configured your settings.
6. Click the big START button to start the machine and try it out!

### Driver Details

* TinyG: https://github.com/openpnp/openpnp/wiki/TinyG
* Grbl: https://github.com/openpnp/openpnp/wiki/Grbl

Heads
-----

### Head Offsets

Head offsets are a complex and important concept in OpenPnP. Head offsets allow you to tell
OpenPnP about the layout of your machine's head and the distances between the various movable
objects on it. You need to set your offsets before you can use OpenPnP.

A typical head will have, at least a nozzle and a camera. Some may have multiple nozzles and may add actuators for tape drag feeders. Each of these objects have an associated head offsets configuration property.

The easiest way to think of head offsets is to imagine the distance that any object on the head must travel to be exactly where another object is. For instance, if the nozzle is touching the bed of the machine and you want to have the camera focused on that exact same spot the head offsets are the distance in X, Y, and Z that the head must move to center the camera, in focus, where the nozzle was touching.

#### Setup

To set up your offsets, follow these steps:

1. Go to the Machine Setup tab, select your primary Nozzle from the tree on the left and set the Offsets to 0, 0, 0 in the fields on the right. Press Apply.
2. Place something on the bed of the machine that can be marked by the nozzle. A piece of double sided tape or a small, flattened blob of Silly Putty will work. This is our target.
3. Jog the machine so that the primary nozzle is over the target and then lower the nozzle until it makes a clear mark on the target.
4. Click the X, Y and Z, DRO one time each. They will turn blue and show 0.000. They are now in relative coordinate mode and will show the distance you have moved since clicking them until you click them again.
5. Jog the machine so that the down-looking camera is over the mark on the target, perfectly centered and in focus.
6. Find the down-looking camera in the Machine Setup tab and find the Offsets fields in the panel on the right. It's currently on the second tab.
7. Set the offsets to the X, Y and Z shown in the DROs. Press Apply.
8. For each additional Nozzle, Camera or Actuator you need to setup, simply jog the machine so that the Nozzle, Camera or Actuator, is focused on (for Cameras) or touching (for Nozzles and Actuators) the mark on the target and record the offsets in the appropriate fields.

#### Testing

To test that your offsets are correct you can use the red positioning buttons next to the DROs:

1. One again, make a mark on the target and leave the nozzle there.
2. Press the position camera button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/position-camera.svg). OpenPnP will move the head so that the mark is now centered in the camera and in perfect focus.
3. Now press the position nozzle button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/position-nozzle.svg) and the nozzle should move back to it's place touching the mark.

Actuators
---------

Cameras
-------
* Add a camera on the "Cameras" tab of "The Tabs" by pressing the green "+" icon.  
* Select a camera driver from the provided list, try the "OpenCVCamera" and press the "Accept" button. The newly added camera will show up in the camera list.
* Double click on the name of the new camera to rename it.  
* Click on the "Camera Specific" tab (while your newly created camera is still selected in the camera list).
* In the "General" section you can pick the "Device Index".  Each camera connected to your computer will have a unique device index starting at index 0. 
* Verify your camera is working, in the "Camera" window select your newly added camera from the drop down list.  If configured correctly you should see a live image from your selected camera.

### Camera Types
* LtiCivilCamera
* VfwCamera
* TableScannerCamera
* OpenCVCamera
* ImageCamera

Feeders
-------

There are currently four types of feeders supported:

1. ReferenceStripFeeder: A feeder that allows the user to place a cut piece of SMT tape on the machine. The feeder will advance along the tape and pick parts from it. Cover film must be removed manually. Supports vision for setup and part centering by referencing the holes in the tape.

2. ReferenceTapeFeeder: A "drag" feeder which allows the use of an Actuator, typically a solenoid with a pin, to advance the tape by dragging it. Basic vision for part center detection is also supported.

3. ReferenceTrayFeeder: Supports 2D arrays of parts in trays. Currently limited to trays that are aligned at 90 degrees in X and Y. Simple incremental pick, no vision.

4. ReferenceTubeFeeder: The simplest feeder which picks from the same location every time. Intended to be used with a vibratory tube feeder that presents a part at the same location repeatedly.

Operation
=========

Parts
-----

Packages
--------

Boards
------

Boards tell OpenPnP which parts to place and where to place them. Boards are stored in files with the extension `.board.xml`. A Board contains a list of Placements. A Placement tells OpenPnP which part to place at what coordinates and rotation.

Board files are independent from any user or machine. You can share Board files for a given PCB design and use the file to build that particular PCB.

You will typically create a new Board file by importing data from your CAD software such as Eagle or KiCAD. Once you've created a Board file for a design there's no need to change it unless the design changes.

Jobs
----

Job files tell OpenPnP where to find one or more Boards on the machine. A Job might consist of a single Board or of many of the same or even different Boards. Each line in a Job tells OpenPnP where to find one particular Board using machine coordinates. When you run the Job OpenPnP will process all of the Placements for each Board in the Job.

Since a Job can contain many of the same Board you can use it to process a full panel of PCBs. Just add the same Board to the Job for as many as the panel contains and set the position of each in the Job.

Your First Job
--------------

This is a brief explanation of how to setup and run your first job. For details on any step you can refer to other parts of this page, but be warned that it hasn't all been documented yet. This is meant to serve as a jumping off point.

1. Define packages used in the PCB in the Packages tab.
2. Define parts used in the PCB in the Parts tab. Reference the previously created Packages.
3. Setup feeders in the Feeders tab for each unique Part being used in the job.
4. Create a new board by pressing ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/file-new.svg) in the Job tab and add Placements to it for each Placement in the PCB.
5. Set the position of the board in the Job tab. You can use capture camera ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/capture-camera.svg) to align it to the corner, use fiducial locate ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/board-fiducial-locate.svg) to find it automatically or use the two placement manual process ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/board-two-placement-locate.svg).

    For additional information on using fiducials, see [[Fiducials]].
6. You'll need to set the Z position of the board, too. Touch the nozzle tip to the board and use capture nozzle ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/capture-nozzle.svg) to set it.
7. Run the job by clicking Start ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/control-start.svg).

Troubleshooting
===============

FAQ
===

Advanced Topics
===============

Configuration Files
-------------------

Configuration files are located in your home directory, under a subdirectory called `.openpnp`.

* On Mac this will typically be `/Users/[username]/.openpnp`.
* On Windows 2000, XP and 2003 it will be `C:\Documents and Settings\[username]\.openpnp`.
* On Windows Vista and above it's `C:\Users\[username]\.openpnp`.

Configuration files are in XML format and can be edited by hand in a text editor. You should shutdown OpenPnP before editing files by hand as OpenPnP will rewrite the configuration files on exit.

There are three primary configuration files. They are:

1. `machine.xml`: Contains the primary configuration for the entire system, including information about the machine, cameras, feeders, nozzles, etc.
2. `parts.xml`: A portable parts database. As you define parts (components) in OpenPnP they are stored here.
3. `packages.xml`: A portable packages database. Component package information including shape and dimensions are stored here.

Custom Implementations and Integration
--------------------------------------

If you are interested in having OpenPnP work with a machine that is not currently supported
you will need an OpenPnP driver that can talk to your hardware and you will need to
configure it in the `machine.xml`.

To get started, look at the list of drivers in the package below to see what drivers are
available and determine if one will meet your needs.

https://github.com/openpnp/openpnp/tree/develop/src/main/java/org/openpnp/machine/reference/driver

If none of those will work for your machine, you will need to write one. Once
you have a driver, you can specify it's classname and configuration parameters
in `machine.xml`.

See the Development section for more information if you decide you need to write code.

Development
-----------

For more information about developing OpenPnP, especially regarding contributing, please see
https://github.com/openpnp/openpnp/wiki/Developers-Guide.

Debugging
---------

Getting Help
============

Discussion Group
----------------

There is an active discussion group at http://groups.google.com/group/openpnp. This will typically be the best place to get help.

IRC
---

We also have an IRC channel on Freenode IRC at #openpnp. If you don't have an IRC client, you can use [this web based one](http://webchat.freenode.net/?channels=openpnp).